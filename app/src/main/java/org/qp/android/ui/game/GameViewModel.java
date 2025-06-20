package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.Base64Util.decodeBase64;
import static org.qp.android.helpers.utils.Base64Util.isBase64;
import static org.qp.android.helpers.utils.ColorUtil.convertRGBAtoBGRA;
import static org.qp.android.helpers.utils.ColorUtil.getHexColor;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromFullPath;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.runOnUiThread;
import static org.qp.android.helpers.utils.ViewUtil.getFontStyle;
import static org.qp.android.ui.game.GameActivity.LOAD;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.R;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.questopiabundle.AsyncCallbacks;
import org.qp.android.questopiabundle.IQuestopiaBundle;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.LibReturnValue;
import org.qp.android.questopiabundle.dto.LibGameState;
import org.qp.android.questopiabundle.dto.LibGenItem;
import org.qp.android.questopiabundle.dto.LibIConfig;
import org.qp.android.questopiabundle.lib.LibGameRequest;
import org.qp.android.questopiabundle.lib.LibRefIRequest;
import org.qp.android.questopiabundle.lib.LibTypeDialog;
import org.qp.android.questopiabundle.lib.LibTypeWindow;
import org.qp.android.ui.dialogs.GameDialogFrags;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.dialogs.GamePopupType;
import org.qp.android.ui.settings.SettingsController;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.subjects.PublishSubject;

@HiltViewModel
public class GameViewModel extends AndroidViewModel {

    private static final String PAGE_HEAD_TEMPLATE = """
            <!DOCTYPE html>
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style type="text/css">
              body {
                margin: 0;
                padding: 0.5em;
                color: QSPTEXTCOLOR;
                background-color: QSPBACKCOLOR;
                font-size: QSPFONTSIZE;
                font-family: QSPFONTSTYLE;
              }
              a { color: QSPLINKCOLOR; }
              a:link { color: QSPLINKCOLOR; }
            </style>
            </head>
            """;
    private static final String PAGE_BODY_TEMPLATE = "<body>REPLACETEXT</body>";
    public final MutableLiveData<List<LibGenItem>> actsListLiveData = new MutableLiveData<>();
    public final MutableLiveData<Boolean> actsVisibility = new MutableLiveData<>();
    public final MutableLiveData<List<LibGenItem>> objsListLiveData = new MutableLiveData<>();
    private final PublishSubject<String> dialogConnector = PublishSubject.create();
    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final PluginClient pluginClient;
    private final AudioPlayer player;
    private final HtmlProcessor processor;
    private final int nativeLibVer;
    private final CompletableFuture<IQuestopiaBundle> serviceReadyFuture = new CompletableFuture<>();
    public String pageTemplate = "";
    public SharedPreferences preferences;
    public Events.Emitter actEmit = new Events.Emitter();
    private Uri gameDirUri;
    private boolean showActions = true;
    private LibGameState libGameState = new LibGameState();
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> {
                controllerObserver.postValue(getSettingsController());
                updatePageTemplate();
                refreshMainDesc();
                refreshVarsDesc();
                refreshActionsRecycler();
                refreshObjectsRecycler();
            };
    private IQuestopiaBundle questopiaBundle = new IQuestopiaBundle.Default();
    private final ServiceConnection engineConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            questopiaBundle = IQuestopiaBundle.Stub.asInterface(service);

            try {
                questopiaBundle.startNativeLib(nativeLibVer);
                initPluginHandler();
                serviceReadyFuture.complete(questopiaBundle);
            } catch (Exception e) {
                runOnUiThread(() -> doShowErrorDialog(e.toString(), ErrorType.EXCEPTION));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questopiaBundle = new IQuestopiaBundle.Default();
        }
    };

    @Inject
    public GameViewModel(@NonNull Application application,
                         @NonNull PluginClient pluginClient,
                         @NonNull HtmlProcessor htmlProcessor,
                         @NonNull AudioPlayer audioPlayer) {
        super(application);

        this.pluginClient = pluginClient;
        this.processor = htmlProcessor;
        this.player = audioPlayer;

        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        nativeLibVer = getSettingsController().nativeLibVersion;
    }

    // region Getter/Setter
    public LibIConfig getIConfig() {
        return libGameState.interfaceConfig;
    }

    public SettingsController getSettingsController() {
        return SettingsController.getInstance(getApplication());
    }

    @SuppressLint("SetJavaScriptEnabled")
    public WebView getDefaultWebClient(WebView view) {
        var webViewClient = new GameWebViewClient();
        view.getSettings().setAllowFileAccess(true);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setUseWideViewPort(true);
        view.getSettings().setBlockNetworkLoads(true);
        view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setWebViewClient(webViewClient);
        return view;
    }

    public LiveData<SettingsController> getControllerObserver() {
        return controllerObserver;
    }

    public int getTextColor() {
        var config = getIConfig();
        if (getSettingsController().isUseGameTextColor && config.fontColor != 0) {
            return convertRGBAtoBGRA((int) config.fontColor);
        } else {
            return getSettingsController().textColor;
        }
    }

    public int getBackgroundColor() {
        var config = getIConfig();
        if (getSettingsController().isUseGameBackgroundColor && config.backColor != 0) {
            return convertRGBAtoBGRA((int) config.backColor);
        } else {
            return getSettingsController().backColor;
        }
    }

    public int getLinkColor() {
        var config = getIConfig();
        if (getSettingsController().isUseGameLinkColor && config.linkColor != 0) {
            return convertRGBAtoBGRA((int) config.linkColor);
        } else {
            return getSettingsController().linkColor;
        }
    }

    public int getFontSize() {
        var config = getIConfig();
        return getSettingsController().isUseGameFont && config.fontSize != 0
                ? (int) config.fontSize
                : getSettingsController().fontSize;
    }

    public String getHtml(String str) {
        var config = getIConfig();
        return config.useHtml ?
                processor.convertLibHtmlToWebHtml(str) :
                processor.convertLibStrToHtml(str);
    }

    public Uri getImageUriFromPath(String src) {
        final var relPath = Uri.parse(src).getPath();
        if (!isNotEmptyOrBlank(relPath)) return Uri.EMPTY;
        final var gameDir = getCurGameDir();
        if (!isWritableDir(getApplication(), gameDir)) return Uri.EMPTY;
        var imageFile = fromRelPath(getApplication(), relPath, gameDir, false);
        if (!isWritableFile(getApplication(), imageFile)) return Uri.EMPTY;
        return imageFile.getUri();
    }

    public LiveData<String> getMainDescObserver() {
        return mainDescLiveData;
    }

    public LiveData<String> getVarsDescObserver() {
        return varsDescLiveData;
    }

    @Nullable
    public DocumentFile getCurGameDir() {
        if (gameDirUri == null) return null;
        return DocumentFileCompat.fromUri(getApplication(), gameDirUri);
    }

    @Nullable
    public DocumentFile getSavesDir() {
        if (!isWritableDir(getApplication(), getCurGameDir())) return null;
        return findOrCreateFolder(getApplication(), getCurGameDir(), "saves");
    }

    // endregion Getter/Setter
    public void doOnStartRWSave(int slotAction) {
        actEmit.emitAndExecute(new GameFragmentNavigation.StartRWSave(slotAction));
    }

    public void doOnFinishActivity() {
        actEmit.emitAndExecute(new GameFragmentNavigation.FinishActivity());
    }

    public void doOnWarnUser(int tabId) {
        actEmit.emitAndExecute(new GameFragmentNavigation.WarnUser(tabId));
    }

    public void doOnShowPopup(GamePopupType type) {
        actEmit.emitAndExecute(new GameFragmentNavigation.ShowPopup(type));
    }

    public void doOnShowDialog(GameDialogFrags buildDialog) {
        actEmit.emitAndExecute(new GameFragmentNavigation.ShowDialog(buildDialog));
    }

    public String removeHtmlTags(String dirtyHTML) {
        return processor.removeHtmlTags(dirtyHTML);
    }

    private boolean isHasHTMLTags(String input) {
        return processor.isContainsHtmlTags(input);
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        var optWindow = Optional.ofNullable(dialog.requireDialog().getWindow());
        if (optWindow.isEmpty()) return;
        if (dialog.getTag() == null) return;

        switch (dialog.getTag()) {
            case "closeDialogFragment" -> {
                stopAudio();
                terminateLibAndPlugin();
                doOnFinishActivity();
            }
            case "inputDialogFragment", "executorDialogFragment" -> {
                var inputBoxEdit = (TextInputLayout) optWindow.get().findViewById(R.id.inputBox_edit);
                var optInputBoxEditET = Optional.ofNullable(inputBoxEdit.getEditText());
                if (optInputBoxEditET.isEmpty()) return;
                var outputText = optInputBoxEditET.get().getText().toString();
                dialogConnector.onNext(outputText);
            }
            case "errorDialogFragment" -> {
                var feedBackName = (TextInputLayout) optWindow.get().findViewById(R.id.feedBackName);
                var feedBackContact = (TextInputLayout) optWindow.get().findViewById(R.id.feedBackContact);
                var feedBackMessage = (TextInputLayout) optWindow.get().findViewById(R.id.feedBackMessage);

                var optFeedBackNameET = Optional.ofNullable(feedBackName.getEditText());
                var optFeedBackContactET = Optional.ofNullable(feedBackContact.getEditText());
                var optFeedBackMessageET = Optional.ofNullable(feedBackMessage.getEditText());
                if (optFeedBackNameET.isEmpty() || optFeedBackContactET.isEmpty()) return;
                var feedBackNameET = optFeedBackNameET.get();
                var feedBackContactET = optFeedBackContactET.get();
                if (optFeedBackMessageET.isPresent()) {
                    var feedBackMessageET = optFeedBackMessageET.get();
                    // TODO: 06.01.2025 Replace to ACRA
//                    Log.d(this.getClass().getSimpleName(), feedBackMessageET.getText().toString()
//                            + "\n" + feedBackContactET.getText().toString()
//                            + "\n" + feedBackNameET.getText().toString());
                } else {
                    // TODO: 06.01.2025 Replace to ACRA
//                    Log.d(this.getClass().getSimpleName(), feedBackContactET.getText().toString()
//                            + "\n" + feedBackNameET.getText().toString());
                }
            }
            case "loadErrorGameDialogFragment" -> doOnStartRWSave(LOAD);
        }
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        var dialogTag = dialog.getTag();
        if (dialogTag != null) {
            switch (dialogTag) {
                case "inputDialogFragment", "executorDialogFragment" -> dialogConnector.onNext("");
                case "menuDialogFragment" -> dialogConnector.onNext(String.valueOf(-1));
            }
        }
    }

    public void onDialogListClick(DialogFragment dialog, int which) {
        var dialogTag = dialog.getTag();
        if (dialogTag != null) {
            if (Objects.equals(dialogTag, "menuDialogFragment")) {
                dialogConnector.onNext(String.valueOf(which));
            }
        }
    }

    public void updatePageTemplate() {
        var pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE", getFontStyle(getSettingsController().getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(getFontSize()));
        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private void refreshMainDesc() {
        var libMainDesc = getHtml(libGameState.mainDesc);
        var dirtyHTML = pageTemplate.replace("REPLACETEXT", libMainDesc);
        var cleanHTML = "";
        if (getSettingsController().isImageDisabled) {
            cleanHTML = processor.getCleanHtmlRemMedia(dirtyHTML);
        } else {
            cleanHTML = processor.getCleanHtmlAndMedia(dirtyHTML, getSettingsController());
        }
        if (!cleanHTML.isBlank()) {
            runOnUiThread(() -> doOnWarnUser(GameActivity.TAB_MAIN_DESC_AND_ACTIONS));
        }
        mainDescLiveData.postValue(cleanHTML);
    }

    private void refreshVarsDesc() {
        var libVarsDesc = getHtml(libGameState.varsDesc);
        var dirtyHTML = pageTemplate.replace("REPLACETEXT", libVarsDesc);
        var cleanHTML = "";
        if (getSettingsController().isImageDisabled) {
            cleanHTML = processor.getCleanHtmlRemMedia(dirtyHTML);
        } else {
            cleanHTML = processor.getCleanHtmlAndMedia(dirtyHTML, getSettingsController());
        }
        if (!cleanHTML.isBlank()) {
            runOnUiThread(() -> doOnWarnUser(GameActivity.TAB_VARS_DESC));
        }
        varsDescLiveData.postValue(cleanHTML);
    }

    public void onActionClicked(int index) {
        try {
            questopiaBundle.onActionClicked(index);
        } catch (RemoteException e) {
            doShowErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    private void refreshActionsRecycler() {
        var actionsList = libGameState.actionsList;
        if (actionsList != null) {
            var countElement = actionsList.size();
            actsVisibility.postValue(showActions && countElement > 0);
            actsListLiveData.postValue(actionsList);
        }
    }

    public void onObjectClicked(int index) {
        try {
            questopiaBundle.onObjectClicked(index);
        } catch (RemoteException e) {
            doShowErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    private void refreshObjectsRecycler() {
        var objectList = libGameState.objectsList;
        if (objectList != null) {
            runOnUiThread(() -> doOnWarnUser(GameActivity.TAB_OBJECTS));
            objsListLiveData.postValue(objectList);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public void pauseAudio() {
        player.pause();
    }

    public void resumeAudio() {
        player.setSoundEnabled(getSettingsController().isSoundEnabled);
        player.resume();
    }

    public void startAudio() {
        player.start();
    }

    public void stopAudio() {
        player.stop();
    }

    private void initPluginHandler() throws CompletionException {
        try {
            questopiaBundle.sendAsync(new AsyncCallbacks.Stub() {
                @Override
                public void updateState(LibResult refReq, LibResult newState) throws RemoteException {
                    libGameState = (LibGameState) newState.value;
                    var libRefIRequest = (LibRefIRequest) refReq.value;
                    doRefresh(libRefIRequest);
                }

                @Override
                public void changeGameDir(String filePath) throws RemoteException {
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> DocumentFileCompat.doesExist(getApplication(), filePath))
                            .thenAcceptAsync(aBoolean -> {
                                if (aBoolean) {
                                    var oldDirPath = documentWrap(gameDir).getAbsolutePath(getApplication());
                                    if (!Objects.equals(oldDirPath, filePath)) {
                                        var newGameDir = fromFullPath(getApplication(), filePath, true);
                                        if (isWritableDir(getApplication(), newGameDir)) {
                                            GameViewModel.this.gameDirUri = newGameDir.getUri();
                                        }
                                    }
                                }
                            });
                }

                @Override
                public void doShowDialog(LibResult typeDialog, String inputString) throws RemoteException {
                    final var libType = (LibTypeDialog) typeDialog.value;
                    CompletableFuture
                            .supplyAsync(() -> switch (libType) {
                                case DIALOG_PICTURE -> {
                                    showLibDialog(normalizeContentPath(inputString), GameDialogType.IMAGE_DIALOG);
                                    yield null;
                                }
                                case DIALOG_MESSAGE -> {
                                    showLibDialog(inputString, GameDialogType.MESSAGE_DIALOG);
                                    yield null;
                                }
                                case DIALOG_ERROR -> {
                                    showLibDialog(inputString, GameDialogType.ERROR_DIALOG_WSEND);
                                    yield null;
                                }
                                case DIALOG_POPUP_SAVE -> {
                                    showLibSavePopup();
                                    yield null;
                                }
                                default -> showLibDialog(libType, inputString);
                            })
                            .thenAcceptAsync(libDialogRetValue -> {
                                if (libDialogRetValue != null) {
                                    try {
                                        questopiaBundle.receiveValue(libDialogRetValue);
                                    } catch (RemoteException e) {
                                        throw new CompletionException(e);
                                    }
                                }
                            })
                            .exceptionally(t -> {
                                runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
                                return null;
                            });
                }

                @Override
                public void doChangeVisWindow(LibResult typeWindow, boolean isShow) throws RemoteException {
                    final var libWindowType = (LibTypeWindow) typeWindow.value;
                    if (libWindowType == LibTypeWindow.ACTIONS) {
                        runOnUiThread(() -> showActions = isShow);
                    }
                }

                @Override
                public void isPlayingFile(String filePath) throws RemoteException {
                    final var normPath = normalizeContentPath(filePath);
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> fromRelPath(getApplication(), normPath, gameDir, false))
                            .thenApplyAsync(soundFile -> {
                                if (isWritableFile(getApplication(), soundFile)) {
                                    return soundFile.getUri();
                                } else {
                                    var errorMsg = String.format("Sound file by path: %s not writable", filePath);
                                    throw new CompletionException(new RuntimeException(errorMsg));
                                }
                            })
                            .exceptionally(throwable -> {
                                if (getSettingsController().isUseMusicDebug) {
                                    doShowErrorDialog(throwable.toString(), ErrorType.SOUND_ERROR);
                                }
                                return Uri.EMPTY;
                            })
                            .thenApply(player::isPlayingFile)
                            .thenAcceptAsync(isPlay -> {
                                if (isPlay != null) {
                                    try {
                                        var returnValue = new LibReturnValue();
                                        returnValue.playFileState = isPlay;
                                        questopiaBundle.receiveValue(returnValue);
                                    } catch (RemoteException e) {
                                        throw new CompletionException(e);
                                    }
                                }
                            })
                            .exceptionally(t -> {
                                runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
                                return null;
                            });
                }

                @Override
                public void closeAllFiles() throws RemoteException {
                    player.closeAllFiles().exceptionally(t -> {
                        runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
                        return null;
                    });
                }

                @Override
                public void closeFile(String filePath) throws RemoteException {
                    final var normPath = normalizeContentPath(filePath);
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> fromRelPath(getApplication(), normPath, gameDir, false))
                            .thenApplyAsync(soundFile -> {
                                if (isWritableFile(getApplication(), soundFile)) {
                                    return soundFile.getUri();
                                } else {
                                    return Uri.EMPTY;
                                }
                            })
                            .thenCompose(uri -> {
                                if (uri != Uri.EMPTY) {
                                    return player.closeFile(uri);
                                } else {
                                    var errorMsg = String.format("Sound file by path: %s not writable", filePath);
                                    throw new CompletionException(new RuntimeException(errorMsg));
                                }
                            })
                            .exceptionally(throwable -> {
                                if (getSettingsController().isUseMusicDebug) {
                                    doShowErrorDialog(throwable.toString(), ErrorType.SOUND_ERROR);
                                }
                                return null;
                            });
                }

                @Override
                public void playFile(String path, int volume) throws RemoteException {
                    final var normPath = normalizeContentPath(path);
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> fromRelPath(getApplication(), normPath, gameDir, false))
                            .thenApplyAsync(soundFile -> {
                                if (isWritableFile(getApplication(), soundFile)) {
                                    return soundFile.getUri();
                                } else {
                                    return Uri.EMPTY;
                                }
                            })
                            .thenCompose(uri -> {
                                if (uri != Uri.EMPTY) {
                                    return player.playFile(getApplication(), uri, volume);
                                } else {
                                    var errorMsg = String.format("Sound file by path: %s not writable", path);
                                    throw new CompletionException(new RuntimeException(errorMsg));
                                }
                            })
                            .exceptionally(throwable -> {
                                if (getSettingsController().isUseMusicDebug) {
                                    doShowErrorDialog(throwable.toString(), ErrorType.SOUND_ERROR);
                                }
                                return null;
                            });
                }

                @Override
                public void requestReceiveFile(String filePath) throws RemoteException {
                    if (!isNotEmptyOrBlank(filePath)) return;
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> DocumentFileCompat.doesExist(getApplication(), filePath))
                            .thenApplyAsync(aBoolean -> {
                                if (aBoolean) {
                                    return fromFullPath(getApplication(), filePath, false);
                                } else {
                                    return fromRelPath(getApplication(), filePath, gameDir, false);
                                }
                            })
                            .thenApplyAsync(receiveFile -> {
                                if (isWritableFile(getApplication(), receiveFile)) {
                                    return receiveFile.getUri();
                                } else {
                                    return Uri.EMPTY;
                                }
                            })
                            .thenAcceptAsync(fileUri -> {
                                if (fileUri == Uri.EMPTY) return;
                                getApplication().grantUriPermission(
                                        "org.qp.android.questopiabundle",
                                        fileUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                | Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                                try {
                                    var returnValue = new LibReturnValue();
                                    returnValue.fileUri = fileUri;
                                    questopiaBundle.receiveValue(returnValue);
                                } catch (RemoteException e) {
                                    throw new CompletionException(e);
                                }
                            })
                            .exceptionally(t -> {
                                runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
                                return null;
                            });
                }

                @Override
                public void requestCreateFile(String path, String mimeType) throws RemoteException {
                    final var gameDir = getCurGameDir();
                    if (!isWritableDir(getApplication(), gameDir)) return;

                    CompletableFuture
                            .supplyAsync(() -> findOrCreateFile(getApplication(), gameDir, path, mimeType))
                            .thenApplyAsync(newFile -> {
                                if (isWritableFile(getApplication(), newFile)) {
                                    return newFile.getUri();
                                } else {
                                    return Uri.EMPTY;
                                }
                            })
                            .thenAcceptAsync(fileUri -> {
                                if (fileUri == Uri.EMPTY) return;
                                getApplication().grantUriPermission(
                                        "org.qp.android.questopiabundle",
                                        fileUri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                | Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );
                                try {
                                    var returnValue = new LibReturnValue();
                                    returnValue.fileUri = fileUri;
                                    questopiaBundle.receiveValue(returnValue);
                                } catch (RemoteException e) {
                                    throw new CompletionException(e);
                                }
                            })
                            .exceptionally(t -> {
                                runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
                                return null;
                            });
                }

                @Override
                public void onError(LibException libException) throws RemoteException {
                    doShowErrorDialog(libException.toException().toString(), ErrorType.EXCEPTION);
                }
            });
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    public boolean checkNativePlugin() {
        return pluginClient.isPluginExist(getApplication(), PluginType.ENGINE_PLUGIN);
    }

    public CompletableFuture<Boolean> initNativePlugin() {
        return pluginClient.connectEnginePlugin(getApplication(), engineConn);
    }

    public void terminateLibAndPlugin() {
        pluginClient.disconnectEnginePlugin(getApplication(), () -> {
            try {
                questopiaBundle.stopNativeLib(nativeLibVer);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).exceptionally(t -> {
            runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
            return false;
        });
    }

    public void runGameIntoNativeLib(long gameId,
                                     String gameTitle,
                                     Uri gameDirUri,
                                     Uri gameFileUri) {
        this.gameDirUri = gameDirUri;

        getApplication().grantUriPermission(
                "org.qp.android.questopiabundle",
                gameFileUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        getApplication().grantUriPermission(
                "org.qp.android.questopiabundle",
                gameDirUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        serviceReadyFuture.thenAccept(bundle -> {
            try {
                bundle.runGameIntoLib(gameId, gameTitle, gameDirUri, gameFileUri);
            } catch (RemoteException e) {
                throw new CompletionException(e);
            }
        }).exceptionally(t -> {
            runOnUiThread(() -> doShowErrorDialog(t.toString(), ErrorType.EXCEPTION));
            return null;
        });
    }

    public void requestForNativeLib(LibGameRequest req, String codeToExec) {
        try {
            questopiaBundle.doLibRequest(new LibResult<>(req), codeToExec, Uri.EMPTY);
        } catch (RemoteException e) {
            runOnUiThread(() -> doShowErrorDialog(e.toString(), ErrorType.EXCEPTION));
        }
    }

    public void requestForNativeLib(LibGameRequest req, Uri fileUri) {
        try {
            getApplication().grantUriPermission(
                    "org.qp.android.questopiabundle",
                    fileUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
            questopiaBundle.doLibRequest(new LibResult<>(req), "", fileUri);
        } catch (RemoteException e) {
            runOnUiThread(() -> doShowErrorDialog(e.toString(), ErrorType.EXCEPTION));
        }
    }

    public void requestForNativeLib(LibGameRequest req) {
        try {
            questopiaBundle.doLibRequest(new LibResult<>(req), "", Uri.EMPTY);
        } catch (RemoteException e) {
            runOnUiThread(() -> doShowErrorDialog(e.toString(), ErrorType.EXCEPTION));
        }
    }

    public Boolean isGameRunning() {
        return libGameState.gameRunning;
    }

    // region GameInterface
    public void doRefresh(final LibRefIRequest request) {
        if (request.isActionsChanged) {
            refreshActionsRecycler();
        }
        if (request.isObjectsChanged) {
            refreshObjectsRecycler();
        }
        if (request.isIConfigChanged || request.isMainDescChanged) {
            updatePageTemplate();
            refreshMainDesc();
        }
        if (request.isIConfigChanged || request.isVarsDescChanged) {
            updatePageTemplate();
            refreshVarsDesc();
        }
    }

    private String convertMessage(String inputStr) {
        var processedMsg = getIConfig().useHtml ? removeHtmlTags(inputStr) : inputStr;
        return processedMsg == null ? "" : processedMsg;
    }

    public LibReturnValue sendInputDialog(String inputStr) {
        final var message = convertMessage(inputStr);

        var dialogFragment = new GameDialogFrags(GameDialogType.INPUT_DIALOG);
        if (message.equals("userInputTitle")) {
            dialogFragment.setMessage(ContextCompat.getString(getApplication(), R.string.userInputTitle));
        } else {
            dialogFragment.setMessage(message);
        }

        runOnUiThread(() -> doOnShowDialog(dialogFragment));

        var textValue = dialogConnector.blockingFirst();
        var wrap = new LibReturnValue();
        wrap.outTextValue = textValue;
        return wrap;
    }

    public LibReturnValue sendExecutorDialog(String inputStr) {
        final var message = convertMessage(inputStr);

        var dialogFragment = new GameDialogFrags(GameDialogType.EXECUTOR_DIALOG);
        if (message.equals("execStringTitle")) {
            dialogFragment.setMessage(ContextCompat.getString(getApplication(), R.string.execStringTitle));
        } else {
            dialogFragment.setMessage(message);
        }

        runOnUiThread(() -> doOnShowDialog(dialogFragment));

        var textValue = dialogConnector.blockingFirst();
        var wrap = new LibReturnValue();
        wrap.outTextValue = textValue;
        return wrap;
    }

    public LibReturnValue sendMenuDialog() {
        final var currentItems = libGameState.menuItemsList;

        final var dialogFragment = new GameDialogFrags(GameDialogType.MENU_DIALOG);
        dialogFragment.setItems(currentItems);

        runOnUiThread(() -> doOnShowDialog(dialogFragment));

        try {
            var item = dialogConnector.blockingFirst();
            var selItem = Integer.parseInt(item);
            var wrap = new LibReturnValue();
            wrap.outNumValue = selItem;
            return wrap;
        } catch (NumberFormatException ex) {
//            showErrorDialog(ex.getMessage(), ErrorType.WAITING_ERROR);
            return new LibReturnValue();
        }
    }

    private String getErrorMessage(String inputString, @NonNull ErrorType errorType) {
        return switch (errorType) {
            case IMAGE_ERROR -> ContextCompat.getString(getApplication(), R.string.notFoundImage) + "\n" + inputString;
            case SOUND_ERROR -> ContextCompat.getString(getApplication(), R.string.notFoundSound) + "\n" + inputString;
            case WAITING_ERROR -> ContextCompat.getString(getApplication(), R.string.waitingError) + "\n" + inputString;
            case WAITING_INPUT_ERROR -> ContextCompat.getString(getApplication(), R.string.waitingInputError) + "\n" + inputString;
            case EXCEPTION -> ContextCompat.getString(getApplication(), R.string.error) + "\n" + inputString;
            default -> "";
        };
    }

    private void doShowErrorDialog(String errorStr, ErrorType errorType) {
        var dialogFragment = new GameDialogFrags(GameDialogType.ERROR_DIALOG_WSEND);

        if (errorType == null) {
            dialogFragment.setMessage(errorStr);
        } else {
            dialogFragment.setMessage(getErrorMessage(errorStr, errorType));
        }

        runOnUiThread(() -> doOnShowDialog(dialogFragment));
    }

    public void showLibDialog(String inputStr, GameDialogType type) {
        var dialogFragment = new GameDialogFrags(type);

        switch (type) {
            case ERROR_DIALOG_WSEND -> dialogFragment.setMessage(inputStr);
            case IMAGE_DIALOG -> dialogFragment.pathToImage = getImageUriFromPath(inputStr);
            case MESSAGE_DIALOG -> dialogFragment.setProcessedMsg(convertMessage(inputStr));
        }

        runOnUiThread(() -> doOnShowDialog(dialogFragment));
    }

    public LibReturnValue showLibDialog(LibTypeDialog dialog, String inputStr) {
        return switch (dialog) {
            case DIALOG_INPUT -> sendInputDialog(inputStr);
            case DIALOG_EXECUTOR -> sendExecutorDialog(inputStr);
            case DIALOG_MENU -> sendMenuDialog();
            default -> null;
        };
    }

    public void showLibSavePopup() {
        runOnUiThread(() -> doOnShowPopup(GamePopupType.SAVE_POPUP));
    }
    // endregion GameInterface

    public class GameWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final var uri = request.getUrl();
            if (uri.getScheme() == null) return false;
            final var uriDecode = Uri.decode(uri.toString());

            switch (uri.getScheme()) {
                case "exec" -> {
                    var tempUriDecode = uriDecode.substring(5);
                    if (isBase64(tempUriDecode)) {
                        tempUriDecode = decodeBase64(uriDecode.substring(5));
                    } else {
                        tempUriDecode = uriDecode.substring(5);
                    }
                    if (isHasHTMLTags(tempUriDecode)) {
                        requestForNativeLib(LibGameRequest.EXECUTE_CODE, removeHtmlTags(tempUriDecode));
                    } else {
                        requestForNativeLib(LibGameRequest.EXECUTE_CODE, tempUriDecode);
                    }
                }
                case "https", "http" -> {
                    var viewLink = new Intent(Intent.ACTION_VIEW, Uri.parse(uriDecode));
                    viewLink.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplication().startActivity(viewLink);
                }
                case "file" -> {
                    try {
                        var tempLink = uri.getScheme().replace("file:/", "https:");
                        var viewLazyLink = new Intent(Intent.ACTION_VIEW, Uri.parse(tempLink));
                        viewLazyLink.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(viewLazyLink);
                    } catch (ActivityNotFoundException e) {
                        doShowErrorDialog(e.getMessage(), ErrorType.EXCEPTION);
                    }
                }
            }

            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
            final var uri = request.getUrl();
            if (uri.getScheme() == null)
                return super.shouldInterceptRequest(view, request);
            if (!uri.getScheme().startsWith("file"))
                return super.shouldInterceptRequest(view, request);

            final var rootDir = getCurGameDir();
            if (!isWritableDir(getApplication(), rootDir))
                return super.shouldInterceptRequest(view, request);

            try {
                if (uri.getPath() == null) throw new NullPointerException();
                final var imageFile = fromRelPath(getApplication(), uri.getPath(), rootDir, false);
                final var extension = MimeType.getMimeTypeFromFileName(imageFile.getName());
                final var in = getApplication().getContentResolver().openInputStream(imageFile.getUri());
                return new WebResourceResponse(extension, null, in);
            } catch (NullPointerException | FileNotFoundException ex) {
                if (getSettingsController().isUseImageDebug) {
                    doShowErrorDialog(uri.getPath(), ErrorType.IMAGE_ERROR);
                }
                return super.shouldInterceptRequest(view, request);
            }
        }
    }
}
