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
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;
import static org.qp.android.helpers.utils.ViewUtil.getFontStyle;
import static org.qp.android.model.plugin.PluginClient.LIB_DELAY;
import static org.qp.android.ui.game.GameActivity.LOAD;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
import androidx.databinding.ObservableBoolean;
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
import org.qp.android.model.plugin.PluginType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.questopiabundle.AsyncCallbacks;
import org.qp.android.questopiabundle.LibDialogRetValue;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.lib.LibGameRequest;
import org.qp.android.questopiabundle.lib.LibGameState;
import org.qp.android.questopiabundle.lib.LibIConfig;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameViewModel extends AndroidViewModel {

    private static final int EMITTER_DELAY = LIB_DELAY;
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
    private final QuestopiaApplication questopiaApplication;
    private final ExecutorService singleService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> actionsListLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> objectsListLiveData = new MutableLiveData<>();
    private final PluginClient pluginClient = PluginClient.getInstance();
    public ObservableBoolean isActionVisible = new ObservableBoolean();
    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);
    public MutableLiveData<GameActivity> activityObserver = new MutableLiveData<>();
    public String pageTemplate = "";
    public SharedPreferences preferences;
    public Events.Emitter emitter = new Events.Emitter();
    private Uri gameDirUri;
    private boolean showActions = true;
    private LibGameState libGameState = new LibGameState();
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, key) -> {
        controllerObserver.postValue(getSettingsController());
        updatePageTemplate();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActionsRecycler();
        refreshObjectsRecycler();
    };
    private volatile int nativeLibVer;

    public GameViewModel(@NonNull Application application) {
        super(application);
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        questopiaApplication = (QuestopiaApplication) getApplication();

        pluginClient.proxyMethod(questopiaApplication, PluginType.ENGINE_PLUGIN, this::initPluginHandler)
                .exceptionally(throwable -> {
                    Log.e(this.getClass().getSimpleName(), "Error", throwable);
                    return null;
                });
    }

    private void initPluginHandler() {
        try {
            pluginClient.questopiaBundle.sendAsync(new AsyncCallbacks.Stub() {
                @Override
                public void sendLibGameState(LibResult libResult) throws RemoteException {
                    libGameState = (LibGameState) libResult.value;
                }

                @Override
                public void sendLibRef(LibResult libResult) throws RemoteException {
                    var libRefIRequest = (LibRefIRequest) libResult.value;
                    doRefresh(libRefIRequest);
                }

                @Override
                public void sendChangeCurrGameDir(Uri gameDirUri) throws RemoteException {
                    // TODO: 06.01.2025 Add necessary functionality
                }

                @Override
                public LibDialogRetValue doOnShowDialog(LibResult typeDialog, String inputString) throws RemoteException {
                    var libType = (LibTypeDialog) typeDialog.value;
                    return showLibDialog(libType, inputString);
                }

                @Override
                public void doChangeVisWindow(LibResult typeWindow, boolean isShow) throws RemoteException {
                    var libWindowType = (LibTypeWindow) typeWindow.value;
                    if (libWindowType == LibTypeWindow.ACTIONS) {
                        showActions = isShow;
                        refreshActionsRecycler();
                    }
                }

                @Override
                public boolean isPlayingFile(String filePath) throws RemoteException {
                    return getAudioPlayer().isPlayingFile(filePath);
                }

                @Override
                public void closeAllFiles() throws RemoteException {
                    getAudioPlayer().closeAllFiles();
                }

                @Override
                public void closeFile(String filePath) throws RemoteException {
                    getAudioPlayer().closeFile(filePath);
                }

                @Override
                public void playFile(String path, int volume) throws RemoteException {
                    getAudioPlayer().playFile(path, volume);
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
                    showErrorDialog(libException.toException().toString(), ErrorType.EXCEPTION);
                }
            });
        } catch (Exception e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    // region Getter/Setter
    private HtmlProcessor getHtmlProcessor() {
        return questopiaApplication.getHtmlProcessor();
    }

    private LibGameState getLibGameState() {
        return libGameState;
    }

    public LibIConfig getIConfig() {
        return getLibGameState().interfaceConfig;
    }

    private AudioPlayer getAudioPlayer() {
        return questopiaApplication.getAudioPlayer();
    }

    public LiveData<String> getAudioErrorObserver() {
        return getAudioPlayer().getIsThrowError();
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
        var libState = getLibGameState();
        if (libState == null) return Color.WHITE;
        var config = libState.interfaceConfig;
        if (getSettingsController().isUseGameTextColor && config.fontColor != 0) {
            return convertRGBAtoBGRA((int) config.fontColor);
        } else {
            return getSettingsController().textColor;
        }
    }

    public int getBackgroundColor() {
        var config = getLibGameState().interfaceConfig;
        if (getSettingsController().isUseGameBackgroundColor && config.backColor != 0) {
            return convertRGBAtoBGRA((int) config.backColor);
        } else {
            return getSettingsController().backColor;
        }
    }

    public int getLinkColor() {
        var config = getLibGameState().interfaceConfig;
        if (getSettingsController().isUseGameLinkColor && config.linkColor != 0) {
            return convertRGBAtoBGRA((int) config.linkColor);
        } else {
            return getSettingsController().linkColor;
        }
    }

    public int getFontSize() {
        var config = getLibGameState().interfaceConfig;
        return getSettingsController().isUseGameFont && config.fontSize != 0
                ? (int) config.fontSize
                : getSettingsController().fontSize;
    }

    public String getHtml(String str) {
        var config = getLibGameState().interfaceConfig;
        return config.useHtml ?
                getHtmlProcessor().convertLibHtmlToWebHtml(str) :
                getHtmlProcessor().convertLibStrToHtml(str);
    }

    public Uri getImageUriFromPath(String src) {
        final var relPath = Uri.parse(src).getPath();
        if (!isNotEmptyOrBlank(relPath)) return Uri.EMPTY;
        final var gameDir = getCurGameDir();
        if (!isWritableDir(getApplication(), gameDir)) return Uri.EMPTY;
        var imageFile = fromRelPath(getApplication(), relPath, gameDir, true);
        return imageFile.getUri();
    }

    public LiveData<String> getMainDescObserver() {
        return mainDescLiveData;
    }

    public LiveData<String> getVarsDescObserver() {
        return varsDescLiveData;
    }

    public LiveData<GameItemRecycler> getObjectsObserver() {
        if (objectsListLiveData.getValue() == null) {
            refreshObjectsRecycler();
        }
        return objectsListLiveData;
    }

    public LiveData<GameItemRecycler> getActionObserver() {
        if (actionsListLiveData.getValue() == null) {
            refreshActionsRecycler();
        }
        return actionsListLiveData;
    }

    @Nullable
    public DocumentFile getCurGameDir() {
        return DocumentFileCompat.fromUri(getApplication(), gameDirUri);
    }

    @Nullable
    public DocumentFile getSavesDir() {
        if (!isWritableDir(getApplication(), getCurGameDir())) return null;
        return findOrCreateFolder(getApplication(), getCurGameDir(), "saves");
    }

    public void setGameDirUri(Uri gameDirUri) {
        this.gameDirUri = gameDirUri;
    }

    // endregion Getter/Setter

    public void doOnStartRWSave(int slotAction) {
        emitter.emitAndExecuteOnce(new GameFragmentNavigation.StartRWSave(slotAction));
    }

    public void doOnFinishActivity() {
        emitter.emitAndExecuteOnce(new GameFragmentNavigation.FinishActivity());
    }

    public void doOnWarnUser(int tabId) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.WarnUser(tabId));
        }, EMITTER_DELAY);
    }

    public void doOnShowSavePopup() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowPopupSave());
        }, EMITTER_DELAY);
    }

    public void doOnShowSimpleDialog(@NonNull String inputString,
                                     @NonNull GameDialogType dialogType,
                                     @Nullable ErrorType errorType) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowSimpleDialog(inputString, dialogType, errorType));
        }, EMITTER_DELAY);
    }

    public void doOnShowMessageDialog(@Nullable String inputString,
                                      @NonNull CountDownLatch latch) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowMessageDialog(inputString, latch));
        }, EMITTER_DELAY);
    }

    public void doOnShowInputDialog(@Nullable String inputString,
                                    @NonNull ArrayBlockingQueue<String> inputQueue) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowInputDialog(inputString, inputQueue));
        }, EMITTER_DELAY);
    }

    public void doOnShowExecutorDialog(@Nullable String inputString,
                                       @NonNull ArrayBlockingQueue<String> inputQueue) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowExecutorDialog(inputString, inputQueue));
        }, EMITTER_DELAY);
    }

    public void doOnShowMenuDialog(@Nullable List<String> inputListString,
                                   @NonNull ArrayBlockingQueue<Integer> inputQueue) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            emitter.emitAndExecuteOnce(new GameFragmentNavigation.ShowMenuDialog(inputListString, inputQueue));
        }, EMITTER_DELAY);

    }

    public String removeHtmlTags(String dirtyHTML) {
        return getHtmlProcessor().removeHtmlTags(dirtyHTML);
    }

    private boolean isHasHTMLTags(String input) {
        return getHtmlProcessor().isContainsHtmlTags(input);
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        var optWindow = Optional.ofNullable(dialog.requireDialog().getWindow());
        if (optWindow.isEmpty()) return;
        if (dialog.getTag() == null) return;

        switch (dialog.getTag()) {
            case "closeGameDialogFragment" -> {
                stopAudio();
                stopNativeLib();
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
        CompletableFuture
                .supplyAsync(() -> getHtml(getLibGameState().mainDesc), singleService)
                .thenAcceptAsync(libMainDesc -> {
                    var dirtyHTML = pageTemplate.replace("REPLACETEXT", libMainDesc);
                    var cleanHTML = "";
                    if (getSettingsController().isImageDisabled) {
                        cleanHTML = getHtmlProcessor().getCleanHtmlRemMedia(dirtyHTML);
                    } else {
                        cleanHTML = getHtmlProcessor().getCleanHtmlAndMedia(getApplication(), dirtyHTML);
                    }
                    if (!cleanHTML.isBlank()) {
                        doOnWarnUser(GameActivity.TAB_MAIN_DESC_AND_ACTIONS);
                    }
                    mainDescLiveData.postValue(cleanHTML);
                }, singleService);
    }

    private void refreshVarsDesc() {
        CompletableFuture
                .supplyAsync(() -> getHtml(getLibGameState().varsDesc), singleService)
                .thenAcceptAsync(libVarsDesc -> {
                    var dirtyHTML = pageTemplate.replace("REPLACETEXT", libVarsDesc);
                    var cleanHTML = "";
                    if (getSettingsController().isImageDisabled) {
                        cleanHTML = getHtmlProcessor().getCleanHtmlRemMedia(dirtyHTML);
                    } else {
                        cleanHTML = getHtmlProcessor().getCleanHtmlAndMedia(getApplication(), dirtyHTML);
                    }
                    if (!cleanHTML.isBlank()) {
                        doOnWarnUser(GameActivity.TAB_VARS_DESC);
                    }
                    varsDescLiveData.postValue(cleanHTML);
                }, singleService);
    }

    public void onActionClicked(int index) {
        try {
            pluginClient.questopiaBundle.onActionClicked(index);
        } catch (RemoteException e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    private void refreshActionsRecycler() {
        CompletableFuture
                .supplyAsync(() -> {
                    var actionsRecycler = new GameItemRecycler();
                    actionsRecycler.setTypeface(getSettingsController().getTypeface());
                    actionsRecycler.setTextSize(getFontSize());
                    actionsRecycler.setTextColor(getTextColor());
                    actionsRecycler.setLinkTextColor(getLinkColor());
                    actionsRecycler.setBackgroundColor(getBackgroundColor());
                    actionsRecycler.submitList(getLibGameState().actionsList);
                    return actionsRecycler;
                }, singleService)
                .thenAcceptAsync(actionsRecycler -> {
                    actionsListLiveData.postValue(actionsRecycler);
                    int count = actionsRecycler.getItemCount();
                    isActionVisible.set(showActions && count > 0);
                }, singleService);
    }

    public void onObjectClicked(int index) {
        try {
            pluginClient.questopiaBundle.onObjectClicked(index);
        } catch (RemoteException e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    private void refreshObjectsRecycler() {
        CompletableFuture
                .supplyAsync(() -> {
                    doOnWarnUser(GameActivity.TAB_OBJECTS);
                    var objectsRecycler = new GameItemRecycler();
                    objectsRecycler.setTypeface(getSettingsController().getTypeface());
                    objectsRecycler.setTextSize(getFontSize());
                    objectsRecycler.setTextColor(getTextColor());
                    objectsRecycler.setLinkTextColor(getLinkColor());
                    objectsRecycler.setBackgroundColor(getBackgroundColor());
                    objectsRecycler.submitList(getLibGameState().objectsList);
                    return objectsRecycler;
                }, singleService)
                .thenAcceptAsync(objectsListLiveData::postValue, singleService);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public void startAudio() {
        getAudioPlayer().start();
    }

    public void pauseAudio() {
        getAudioPlayer().pause();
    }

    public void resumeAudio() {
        final var gameDir = getCurGameDir();
        if (isWritableDir(getApplication(), gameDir)) return;

        getAudioPlayer().setCurGameDir(gameDir);
        getAudioPlayer().setSoundEnabled(getSettingsController().isSoundEnabled);
        getAudioPlayer().resume();
    }

    public void stopAudio() {
        getAudioPlayer().stop();
    }

    public void startNativeLib() {
        nativeLibVer = getSettingsController().nativeLibVersion;
        pluginClient.proxyMethod(questopiaApplication, PluginType.ENGINE_PLUGIN, () -> {
            try {
                pluginClient.questopiaBundle.startNativeLib(nativeLibVer);
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Error: ", e);
            }
        }).exceptionally(throwable -> {
            Log.e(this.getClass().getSimpleName(), "Error", throwable);
            return null;
        });
    }

    public void stopNativeLib() {
        pluginClient.proxyMethod(questopiaApplication, PluginType.ENGINE_PLUGIN, () -> {
            try {
                pluginClient.questopiaBundle.stopNativeLib(nativeLibVer);
                pluginClient.disconnectPlugin(getApplication(), PluginType.ENGINE_PLUGIN);
            } catch (Exception e) {
                showErrorDialog(e.toString(), ErrorType.EXCEPTION);
            }
        }).exceptionally(throwable -> {
            Log.e(this.getClass().getSimpleName(), "Error", throwable);
            return null;
        });
    }

    public void runGameIntoNativeLib(long gameId,
                                     String gameTitle,
                                     DocumentFile gameDir,
                                     DocumentFile gameFile) {
        pluginClient.proxyMethod(questopiaApplication, PluginType.ENGINE_PLUGIN, () -> {
            try {
                getApplication().grantUriPermission(
                        "org.qp.android.questopiabundle",
                        gameFile.getUri(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                getApplication().grantUriPermission(
                        "org.qp.android.questopiabundle",
                        gameDir.getUri(),
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                pluginClient.questopiaBundle.runGameIntoLib(gameId, gameTitle, gameDir.getUri(), gameFile.getUri());
            } catch (Exception e) {
                showErrorDialog(e.toString(), ErrorType.EXCEPTION);
            }
        }).exceptionally(throwable -> {
            Log.e(this.getClass().getSimpleName(), "Error", throwable);
            return null;
        });
    }

    public void requestForNativeLib(LibGameRequest req, String codeToExec) {
        try {
            pluginClient.questopiaBundle.doLibRequest(new LibResult<>(req), codeToExec, Uri.EMPTY);
        } catch (Exception e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    public void requestForNativeLib(LibGameRequest req, Uri fileUri) {
        try {
            pluginClient.questopiaBundle.doLibRequest(new LibResult<>(req), "", fileUri);
        } catch (Exception e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    public void requestForNativeLib(LibGameRequest req) {
        try {
            pluginClient.questopiaBundle.doLibRequest(new LibResult<>(req), "", Uri.EMPTY);
        } catch (Exception e) {
            showErrorDialog(e.toString(), ErrorType.EXCEPTION);
        }
    }

    public Boolean isGameRunning() {
        if (getLibGameState() == null) return false;
        return getLibGameState().gameRunning;
    }

    // region GameInterface
    public void doRefresh(final LibRefIRequest request) {
        if (request.isIConfigChanged) {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    emitter.emitAndExecuteOnce(new GameFragmentNavigation.ApplySettings()), LIB_DELAY);
        }
        if (request.isActionsChanged) {
            new Handler(Looper.getMainLooper()).postDelayed(this::refreshActionsRecycler, LIB_DELAY);
        }
        if (request.isObjectsChanged) {
            new Handler(Looper.getMainLooper()).postDelayed(this::refreshObjectsRecycler, LIB_DELAY);
        }
        if (request.isIConfigChanged || request.isMainDescChanged) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updatePageTemplate();
                refreshMainDesc();
            }, LIB_DELAY);
        }
        if (request.isIConfigChanged || request.isVarsDescChanged) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updatePageTemplate();
                refreshVarsDesc();
            }, LIB_DELAY);
        }
    }

    public LibDialogRetValue showLibDialog(LibTypeDialog dialog, String inputString) {
        switch (dialog) {
            case DIALOG_POPUP_SAVE -> doOnShowSavePopup();
            case DIALOG_ERROR ->
                    doOnShowSimpleDialog(inputString, GameDialogType.ERROR_DIALOG, null);
            case DIALOG_PICTURE ->
                    doOnShowSimpleDialog(inputString, GameDialogType.IMAGE_DIALOG, null);
            case DIALOG_POPUP_LOAD ->
                    doOnShowSimpleDialog("", GameDialogType.LOAD_DIALOG, null);
            case DIALOG_MESSAGE -> {
                assertNonUiThread();

                final var latch = new CountDownLatch(1);
                doOnShowMessageDialog(inputString, latch);
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_ERROR);
                }
            }
            case DIALOG_INPUT -> {
                assertNonUiThread();

                final var inputQueue = new ArrayBlockingQueue<String>(1);
                doOnShowInputDialog(inputString, inputQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = inputQueue.take();
                    return wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_INPUT_ERROR);
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = "";
                    return wrap;
                }
            }
            case DIALOG_EXECUTOR -> {
                assertNonUiThread();

                final var inputQueue = new ArrayBlockingQueue<String>(1);
                doOnShowExecutorDialog(inputString, inputQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = inputQueue.take();
                    return wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_INPUT_ERROR);
                    var wrap = new LibDialogRetValue();
                    wrap.outTextValue = "";
                    return wrap;
                }
            }
            case DIALOG_MENU -> {
                assertNonUiThread();

                final var resultQueue = new ArrayBlockingQueue<Integer>(1);
                final var currentItems = getLibGameState().menuItemsList;
                final var newItems = new ArrayList<String>();

                currentItems.forEach(libMenuItem -> newItems.add(libMenuItem.name));
                doOnShowMenuDialog(newItems, resultQueue);
                try {
                    var wrap = new LibDialogRetValue();
                    wrap.outNumValue = resultQueue.take();
                    return wrap;
                } catch (InterruptedException ex) {
                    showErrorDialog(ex.getMessage(), ErrorType.WAITING_ERROR);
                    var wrap = new LibDialogRetValue();
                    wrap.outNumValue = -1;
                    return wrap;
                }
            }
        }
        return null;
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
