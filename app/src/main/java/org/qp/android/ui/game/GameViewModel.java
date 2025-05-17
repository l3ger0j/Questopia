package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.Base64Util.decodeBase64;
import static org.qp.android.helpers.utils.Base64Util.isBase64;
import static org.qp.android.helpers.utils.ColorUtil.convertRGBAtoBGRA;
import static org.qp.android.helpers.utils.ColorUtil.getHexColor;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.PathUtil.getExtension;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginService;
import org.qp.android.model.plugin.PluginType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.questopiabundle.AsyncCallbacks;
import org.qp.android.questopiabundle.IQuestopiaBundle;
import org.qp.android.questopiabundle.LibDialogRetValue;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.dto.LibGameState;
import org.qp.android.questopiabundle.dto.LibGenItem;
import org.qp.android.questopiabundle.dto.LibIConfig;
import org.qp.android.questopiabundle.lib.LibGameRequest;
import org.qp.android.questopiabundle.lib.LibRefIRequest;
import org.qp.android.questopiabundle.lib.LibTypeDialog;
import org.qp.android.questopiabundle.lib.LibTypeWindow;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.settings.SettingsController;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

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
    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final PluginClient pluginClient = PluginService.client;
    private final AudioPlayer player;
    private final HtmlProcessor processor;
    private final int nativeLibVer;
    private final CompletableFuture<IQuestopiaBundle> serviceReadyFuture = new CompletableFuture<>();
    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);
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
    private IQuestopiaBundle questopiaBundle = null;
    private final ServiceConnection engineConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            questopiaBundle = IQuestopiaBundle.Stub.asInterface(service);

            pluginClient.proxyPluginMethods(() -> {
                try {
                    questopiaBundle.startNativeLib(nativeLibVer);
                    initPluginHandler();
                    serviceReadyFuture.complete(questopiaBundle);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }).exceptionally(t -> {
                Log.e(this.getClass().getSimpleName(), "Error: ", t);
                return null;
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questopiaBundle = new IQuestopiaBundle.Default();
        }
    };

    public GameViewModel(@NonNull Application application) {
        super(application);

        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        nativeLibVer = getSettingsController().nativeLibVersion;

        var app = (QuestopiaApplication) application;
        this.processor = app.htmlProcessor;
        this.player = app.audioPlayer;
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
        var webClientSettings = view.getSettings();
        webClientSettings.setAllowFileAccess(true);
        webClientSettings.setJavaScriptEnabled(true);
        webClientSettings.setUseWideViewPort(true);
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setWebViewClient(webViewClient);
        return view;
    }

    public LiveData<SettingsController> getControllerObserver() {
        return controllerObserver;
    }

    public int getTextColor() {
        var libState = libGameState;
        if (libState == null) return Color.WHITE;
        var config = libState.interfaceConfig;
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
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.StartRWSave(slotAction));
    }

    public void doOnFinishActivity() {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.FinishActivity());
    }

    public void doOnWarnUser(int tabId) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.WarnUser(tabId));
    }

    public void doOnShowSavePopup() {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowPopupSave());
    }

    public void doOnShowSimpleDialog(@NonNull String inputString,
                                     @NonNull GameDialogType dialogType,
                                     @Nullable ErrorType errorType) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowSimpleDialog(inputString, dialogType, errorType));
    }

    public void doOnShowMessageDialog(@Nullable String inputString,
                                      @NonNull CountDownLatch latch) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowMessageDialog(inputString, latch));
    }

    public void doOnShowInputDialog(@Nullable String inputString,
                                    @NonNull ArrayBlockingQueue<String> inputQueue) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowInputDialog(inputString, inputQueue));
    }

    public void doOnShowExecutorDialog(@Nullable String inputString,
                                       @NonNull ArrayBlockingQueue<String> inputQueue) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowExecutorDialog(inputString, inputQueue));
    }

    public void doOnShowMenuDialog(@Nullable List<String> inputListString,
                                   @NonNull ArrayBlockingQueue<Integer> inputQueue) {
        actEmit.emitAndExecuteOnce(new GameFragmentNavigation.ShowMenuDialog(inputListString, inputQueue));
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
            case "closeGameDialogFragment" -> {
                stopAudio();
                terminateLibAndPlugin();
                doOnFinishActivity();
            }
            case "inputDialogFragment", "executorDialogFragment" -> {
                var inputBoxEdit = (TextInputLayout) optWindow.get().findViewById(R.id.inputBox_edit);
                var optInputBoxEditET = Optional.ofNullable(inputBoxEdit.getEditText());
                if (optInputBoxEditET.isEmpty()) return;
                var outputText = optInputBoxEditET.get().getText().toString();
                if (Objects.equals(outputText, "")) {
                    outputTextObserver.setValue("");
                } else {
                    outputTextObserver.setValue(outputText);
                }
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
            case "loadGameDialogFragment" -> doOnStartRWSave(LOAD);
            case "showMessageDialogFragment" -> outputBooleanObserver.setValue(true);
        }
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        if (dialog.getTag() != null) {
            if (dialog.getTag().equals("showMenuDialogFragment")) {
                outputIntObserver.setValue(-1);
            }
        }
    }

    public void onDialogListClick(DialogFragment dialog, int which) {
        if (dialog.getTag() != null) {
            if (Objects.equals(dialog.getTag(), "showMenuDialogFragment")) {
                outputIntObserver.setValue(which);
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
            Log.e(this.getClass().getSimpleName(), "Error: ", e);
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
            Log.e(this.getClass().getSimpleName(), "Error: ", e);
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
                public void sendChangeCurrGameDir(Uri gameDirUri) throws RemoteException {
                    var oldValue = GameViewModel.this.gameDirUri;
                    if (!Objects.equals(oldValue, gameDirUri)) {
                        GameViewModel.this.gameDirUri = gameDirUri;
                    }
                }

                @Override
                public LibDialogRetValue doOnShowDialog(LibResult typeDialog, String inputString) throws RemoteException {
                    final var libType = (LibTypeDialog) typeDialog.value;
                    try {
                        return CompletableFuture
                                .supplyAsync(() -> switch (libType) {
                                    case DIALOG_PICTURE ->
                                            showLibDialog(libType, normalizeContentPath(inputString));
                                    default -> showLibDialog(libType, inputString);
                                }, ContextCompat.getMainExecutor(getApplication()))
                                .get();
                    } catch (Exception e) {
                        Log.e(GameViewModel.this.getClass().getSimpleName(), "Error: " + e);
                        return new LibDialogRetValue();
                    }
                }

                @Override
                public void doChangeVisWindow(LibResult typeWindow, boolean isShow) throws RemoteException {
                    final var libWindowType = (LibTypeWindow) typeWindow.value;
                    if (libWindowType == LibTypeWindow.ACTIONS) {
                        runOnUiThread(() -> showActions = isShow);
                    }
                }

                @Override
                public boolean isPlayingFile(String filePath) throws RemoteException {
                    final var normPath = normalizeContentPath(filePath);
                    final var gameDir = getCurGameDir();
                    if (isWritableFile(getApplication(), gameDir)) {
                        var soundFile = fromRelPath(getApplication(), normPath, gameDir, false);
                        if (isWritableFile(getApplication(), soundFile)) {
                            return player.isPlayingFile(soundFile.getUri());
                        } else {
                            if (getSettingsController().isUseMusicDebug) {
                                runOnUiThread(() -> showErrorDialog(filePath, ErrorType.SOUND_ERROR));
                            }
                        }
                    }
                    return false;
                }

                @Override
                public void closeAllFiles() throws RemoteException {
                    player.closeAllFiles().exceptionally(throwable -> {
                        Log.e(GameViewModel.this.getClass().getSimpleName(), "Error: ", throwable);
                        return null;
                    });
                }

                @Override
                public void closeFile(String filePath) throws RemoteException {
                    final var normPath = normalizeContentPath(filePath);
                    final var gameDir = getCurGameDir();
                    if (isWritableFile(getApplication(), gameDir)) {
                        var soundFile = fromRelPath(getApplication(), normPath, gameDir, false);
                        if (isWritableFile(getApplication(), soundFile)) {
                            player.closeFile(soundFile.getUri()).exceptionally(throwable -> {
                                Log.e(GameViewModel.this.getClass().getSimpleName(), "Error: ", throwable);
                                return null;
                            });
                        } else {
                            if (getSettingsController().isUseMusicDebug) {
                                runOnUiThread(() -> showErrorDialog(filePath, ErrorType.SOUND_ERROR));
                            }
                        }
                    }
                }

                @Override
                public void playFile(String path, int volume) throws RemoteException {
                    final var normPath = normalizeContentPath(path);
                    final var gameDir = getCurGameDir();
                    if (isWritableDir(getApplication(), gameDir)) {
                        var soundFile = fromRelPath(getApplication(), normPath, gameDir, false);
                        if (isWritableFile(getApplication(), soundFile)) {
                            player.playFile(getApplication(), soundFile, volume).exceptionally(throwable -> {
                                Log.e(GameViewModel.this.getClass().getSimpleName(), "Error: ", throwable);
                                return null;
                            });
                        } else {
                            if (getSettingsController().isUseMusicDebug) {
                                runOnUiThread(() -> showErrorDialog(path, ErrorType.SOUND_ERROR));
                            }
                        }
                    }
                }

                @Override
                public void requestPermOnFile(Uri fileUri) throws RemoteException {
                    getApplication().grantUriPermission(
                            "org.qp.android.questopiabundle",
                            fileUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                }

                @Override
                public Uri requestCreateFile(Uri fileUri, String path) throws RemoteException {
                    var dir = DocumentFileCompat.fromUri(getApplication(), fileUri);
                    if (isWritableDir(getApplication(), dir)) {
                        var file = findOrCreateFile(getApplication(), dir, path, MimeType.TEXT);
                        if (isWritableFile(getApplication(), file)) {
                            getApplication().grantUriPermission(
                                    "org.qp.android.questopiabundle",
                                    file.getUri(),
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            | Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                            return file.getUri();
                        }
                    }
                    return Uri.EMPTY;
                }

                @Override
                public void onError(LibException libException) throws RemoteException {
                    runOnUiThread(() -> showErrorDialog(libException.toException().toString(), ErrorType.EXCEPTION));
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
        pluginClient.proxyPluginMethods(() -> {
            try {
                questopiaBundle.stopNativeLib(nativeLibVer);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenCombine(pluginClient.disconnectEnginePlugin(getApplication()), (Void, aBool) -> {
            if (!aBool) {
                throw new CompletionException(new Exception("Error disconnect plugin!"));
            } else {
                return true;
            }
        }).exceptionally(throwable -> {
            Log.e(GameViewModel.this.getClass().getSimpleName(), "Error: ", throwable);
            return null;
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

        serviceReadyFuture.thenCompose(bundle -> pluginClient.proxyPluginMethods(() -> {
            try {
                bundle.runGameIntoLib(gameId, gameTitle, gameDirUri, gameFileUri);
            } catch (RemoteException e) {
                throw new CompletionException(e);
            }
        })).exceptionally(throwable -> {
            Log.e(this.getClass().getSimpleName(), "Error: ", throwable);
            return null;
        });
    }

    public void requestForNativeLib(LibGameRequest req, String codeToExec) {
        try {
            questopiaBundle.doLibRequest(new LibResult<>(req), codeToExec, Uri.EMPTY);
        } catch (RemoteException e) {
            Log.e(this.getClass().getSimpleName(), "Error: ", e);
        }
    }

    public void requestForNativeLib(LibGameRequest req, Uri fileUri) {
        try {
            questopiaBundle.doLibRequest(new LibResult<>(req), "", fileUri);
        } catch (RemoteException e) {
            Log.e(this.getClass().getSimpleName(), "Error: ", e);
        }
    }

    public void requestForNativeLib(LibGameRequest req) {
        try {
            questopiaBundle.doLibRequest(new LibResult<>(req), "", Uri.EMPTY);
        } catch (RemoteException e) {
            Log.e(this.getClass().getSimpleName(), "Error: ", e);
        }
    }

    public Boolean isGameRunning() {
        if (libGameState == null) return false;
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

    public LibDialogRetValue showLibDialog(LibTypeDialog dialog, String inputString) {
        assertNonUiThread();
        return switch (dialog) {
            case DIALOG_POPUP_SAVE -> {
                doOnShowSavePopup();
                yield null;
            }
            case DIALOG_ERROR -> {
                doOnShowSimpleDialog(inputString, GameDialogType.ERROR_DIALOG, null);
                yield null;
            }
            case DIALOG_PICTURE -> {
                doOnShowSimpleDialog(inputString, GameDialogType.IMAGE_DIALOG, null);
                yield null;
            }
            case DIALOG_POPUP_LOAD -> {
                doOnShowSimpleDialog("", GameDialogType.LOAD_DIALOG, null);
                yield null;
            }
            case DIALOG_MESSAGE -> {
                final var latch = new CountDownLatch(1);
                doOnShowMessageDialog(inputString, latch);
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_ERROR);
                }
                yield null;
            }
            case DIALOG_INPUT -> {
                final var inputQueue = new ArrayBlockingQueue<String>(1);
                doOnShowInputDialog(inputString, inputQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = inputQueue.take();
                    yield wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_INPUT_ERROR);
                    yield new LibDialogRetValue();
                }
            }
            case DIALOG_EXECUTOR -> {
                final var inputQueue = new ArrayBlockingQueue<String>(1);
                doOnShowExecutorDialog(inputString, inputQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = inputQueue.take();
                    yield wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_INPUT_ERROR);
                    yield new LibDialogRetValue();
                }
            }
            case DIALOG_MENU -> {
                final var resultQueue = new ArrayBlockingQueue<Integer>(1);
                final var currentItems = libGameState.menuItemsList;
                final var newItems = new ArrayList<String>();

                currentItems.forEach(libMenuItem -> newItems.add(libMenuItem.text));
                doOnShowMenuDialog(newItems, resultQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outNumValue = resultQueue.take();
                    yield wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_ERROR);
                    yield new LibDialogRetValue();
                }
            }
        };
    }

    public void showErrorDialog(final String message, final ErrorType errorType) {
        doOnShowSimpleDialog(message, GameDialogType.ERROR_DIALOG, errorType);
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
                        showErrorDialog(e.getMessage(), ErrorType.EXCEPTION);
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
            if (!isWritableDir(getApplication(), getCurGameDir()))
                return super.shouldInterceptRequest(view, request);

            try {
                if (uri.getPath() == null) throw new NullPointerException();
                var imageFile = fromRelPath(getApplication(), uri.getPath(), rootDir, false);
                var extension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(imageFile));
                var in = getApplication().getContentResolver().openInputStream(imageFile.getUri());
                return new WebResourceResponse(extension, null, in);
            } catch (NullPointerException | FileNotFoundException ex) {
                if (getSettingsController().isUseImageDebug) {
                    showErrorDialog(uri.getPath(), ErrorType.IMAGE_ERROR);
                }
                return null;
            }
        }
    }
}
