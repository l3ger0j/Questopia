package org.qp.android.presentation.game;

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
import static org.qp.android.presentation.game.GameActivity.LOAD;

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
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.R;
import org.qp.android.domain.repository.PluginRepository;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
import org.qp.android.helpers.service.AudioPlayer;
import org.qp.android.helpers.service.HtmlProcessor;
import org.qp.android.presentation.dialogs.GameDialogFragBuilder;
import org.qp.android.presentation.dialogs.GameDialogType;
import org.qp.android.presentation.dialogs.GamePopupType;
import org.qp.android.presentation.settings.SettingsController;
import org.qp.android.questopiabundle.AsyncCallbacks;
import org.qp.android.questopiabundle.IQuestopiaBundle;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.LibReturnValue;
import org.qp.android.questopiabundle.dto.LibGameState;
import org.qp.android.questopiabundle.dto.LibGenItem;
import org.qp.android.questopiabundle.dto.LibUIConfig;
import org.qp.android.questopiabundle.lib.LibGameRequest;
import org.qp.android.questopiabundle.lib.LibTypeDialog;
import org.qp.android.questopiabundle.lib.LibTypeWindow;

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
    final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final PublishSubject<String> dialogConnector = PublishSubject.create();
    private final PluginRepository pluginRepository;
    private final AudioPlayer player;
    private final HtmlProcessor processor;
    private final int nativeLibVer;
    private final CompletableFuture<IQuestopiaBundle> serviceReadyFuture = new CompletableFuture<>();
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (sharedPreferences, key) -> controllerObserver.postValue(getSettingsController());
    public SharedPreferences preferences;
    public Events.Emitter actEmit = new Events.Emitter();
    private Uri gameDirUri;
    private boolean showActions = true;
    private LibGameState libGameState = new LibGameState();
    private LibUIConfig libUIConfig = new LibUIConfig();
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
                         @NonNull PluginRepository pluginRepository,
                         @NonNull HtmlProcessor htmlProcessor,
                         @NonNull AudioPlayer audioPlayer) {
        super(application);

        this.pluginRepository = pluginRepository;
        this.processor = htmlProcessor;
        this.player = audioPlayer;

        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        nativeLibVer = getSettingsController().nativeLibVersion;
    }

    // region Getter/Setter
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

    public GameItemAdapter getDefaultItemAdapter(GameItemAdapter adapter) {
        adapter.typeface = getSettingsController().getTypeface();
        adapter.textSize = getFontSize();
        adapter.textColor = getTextColor();
        adapter.linkTextColor = getLinkColor();
        return adapter;
    }

    public int getTextColor() {
        var config = libUIConfig;
        if (getSettingsController().isUseGameTextColor && config.fontColor != 0) {
            return convertRGBAtoBGRA((int) config.fontColor);
        } else {
            return getSettingsController().textColor;
        }
    }

    public int getBackgroundColor() {
        var config = libUIConfig;
        if (getSettingsController().isUseGameBackgroundColor && config.backColor != 0) {
            return convertRGBAtoBGRA((int) config.backColor);
        } else {
            return getSettingsController().backColor;
        }
    }

    public int getLinkColor() {
        var config = libUIConfig;
        if (getSettingsController().isUseGameLinkColor && config.linkColor != 0) {
            return convertRGBAtoBGRA((int) config.linkColor);
        } else {
            return getSettingsController().linkColor;
        }
    }

    public int getFontSize() {
        var config = libUIConfig;
        return getSettingsController().isUseGameFont && config.fontSize != 0
                ? (int) config.fontSize
                : getSettingsController().fontSize;
    }

    public String getHtml(String str) {
        var config = libUIConfig;
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

    public void doOnShowDialog(GameDialogFragBuilder buildDialog) {
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
            case "inputDialogFragment" -> {
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

    @NonNull
    private String createPageTemplate() {
        var pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR", getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR", getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR", getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE", getFontStyle(getSettingsController().getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(getFontSize()));
        return pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private void refreshMainDesc(final String libMainDesc) {
        final var dirtyHTML = createPageTemplate().replace("REPLACETEXT", libMainDesc);
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

    private void refreshVarsDesc(final String libVarsDesc) {
        final var dirtyHTML = createPageTemplate().replace("REPLACETEXT", libVarsDesc);
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

    private void refreshActionsRecycler(final List<LibGenItem> actionsList) {
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

    private void refreshObjectsRecycler(final List<LibGenItem> objectList) {
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
                public void updateState(LibResult newState) throws RemoteException {
                    if (newState != null) {
                        var state = (LibGameState) newState.value;
                        if (state.actionsList != libGameState.actionsList) {
                            refreshActionsRecycler(state.actionsList);
                        }
                        if (state.objectsList != libGameState.objectsList) {
                            refreshObjectsRecycler(state.objectsList);
                        }
                        if (!Objects.equals(state.mainDesc, libGameState.mainDesc)) {
                            refreshMainDesc(getHtml(state.mainDesc));
                        }
                        if (!Objects.equals(state.varsDesc, libGameState.varsDesc)) {
                            refreshVarsDesc(getHtml(state.varsDesc));
                        }
                        if (!Objects.equals(state, libGameState)) {
                            libGameState = (LibGameState) newState.value;
                        }
                    }
                }

                @Override
                public void updateUI(LibResult newConfig) throws RemoteException {
                    if (newConfig != null) {
                        libUIConfig = (LibUIConfig) newConfig.value;
                    }
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
                                if (fileUri != Uri.EMPTY) {
                                    getApplication().grantUriPermission(
                                            "org.qp.android.questopiabundle",
                                            fileUri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    );
                                }
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
        return pluginRepository.isPluginExist(getApplication());
    }

    public CompletableFuture<Boolean> initNativePlugin() {
        return pluginRepository.connectEnginePlugin(getApplication(), engineConn);
    }

    public void terminateLibAndPlugin() {
        pluginRepository.disconnectEnginePlugin(getApplication(), () -> {
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
    private LibReturnValue sendInputDialog(final String inputStr) {
        final var dialogBuilder = new GameDialogFragBuilder(GameDialogType.INPUT_DIALOG);
        final var replaceStr = ContextCompat.getString(getApplication(), R.string.execStringTitle);
        dialogBuilder.inputStr = inputStr.equals("userInputTitle") ? replaceStr : inputStr;
        runOnUiThread(() -> doOnShowDialog(dialogBuilder));
        final var textValue = dialogConnector.blockingFirst();
        final var wrap = new LibReturnValue();
        wrap.dialogTextValue = textValue;
        return wrap;
    }

    private LibReturnValue sendMenuDialog() {
        final var dialogBuilder = new GameDialogFragBuilder(GameDialogType.MENU_DIALOG);
        dialogBuilder.listGenItems = libGameState.menuItemsList;
        runOnUiThread(() -> doOnShowDialog(dialogBuilder));

        try {
            final var item = dialogConnector.blockingFirst();
            final var selItem = Integer.parseInt(item);
            final var wrap = new LibReturnValue();
            wrap.dialogNumValue = selItem;
            return wrap;
        } catch (NumberFormatException ex) {
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
        final var dialogBuilder = new GameDialogFragBuilder(GameDialogType.ERROR_DIALOG_WSEND);
        dialogBuilder.inputStr = errorType == null ? errorStr : getErrorMessage(errorStr, errorType);
        runOnUiThread(() -> doOnShowDialog(dialogBuilder));
    }

    public void showLibDialog(String inputStr, GameDialogType type) {
        final var dialogBuilder = new GameDialogFragBuilder(type);
        switch (type) {
            case ERROR_DIALOG_WSEND, MESSAGE_DIALOG -> dialogBuilder.inputStr = inputStr;
            case IMAGE_DIALOG -> dialogBuilder.imageUri = getImageUriFromPath(inputStr);
        }
        runOnUiThread(() -> doOnShowDialog(dialogBuilder));
    }

    public LibReturnValue showLibDialog(LibTypeDialog dialog, String inputStr) {
        return switch (dialog) {
            case DIALOG_INPUT -> sendInputDialog(inputStr);
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
