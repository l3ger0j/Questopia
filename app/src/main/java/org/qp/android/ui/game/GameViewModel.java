package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.Base64Util.decodeBase64;
import static org.qp.android.helpers.utils.Base64Util.isBase64;
import static org.qp.android.helpers.utils.ColorUtil.convertRGBAtoBGRA;
import static org.qp.android.helpers.utils.ColorUtil.getHexColor;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.getFileContents;
import static org.qp.android.helpers.utils.PathUtil.getExtension;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;
import static org.qp.android.helpers.utils.ViewUtil.getFontStyle;
import static org.qp.android.ui.game.GameActivity.LOAD;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
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
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.TwoQCache;
import org.qp.android.model.lib.LibGameState;
import org.qp.android.model.lib.LibIConfig;
import org.qp.android.model.lib.LibIProxy;
import org.qp.android.model.lib.LibRefIRequest;
import org.qp.android.model.lib.LibWindowType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.settings.SettingsController;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameViewModel extends AndroidViewModel implements GameInterface {

    private final String TAG = this.getClass().getSimpleName();

    private final QuestopiaApplication questopiaApplication;
    private Uri gameDirUri;

    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ObservableBoolean isActionVisible = new ObservableBoolean();
    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);
    public MutableLiveData<GameActivity> activityObserver = new MutableLiveData<>();

    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> actionsListLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> objectsListLiveData = new MutableLiveData<>();

    private static final String PAGE_HEAD_TEMPLATE = """
            <!DOCTYPE html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1">
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
    public String pageTemplate = "";
    public SharedPreferences preferences;
    private boolean showActions = true;

    private final Handler counterHandler = new Handler();

    private int counterInterval = 500;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            if (getLibProxy() == null) return;
            getLibProxy().executeCounter();
            counterHandler.postDelayed(this , counterInterval);
        }
    };

    // region Getter/Setter
    private HtmlProcessor getHtmlProcessor() {
        return questopiaApplication.getHtmlProcessor();
    }

    private LibIProxy getLibProxy() {
        return questopiaApplication.getLibProxy();
    }

    private LibGameState getLibGameState() {
        return getLibProxy().getGameState();
    }

    private AudioPlayer getAudioPlayer() {
        return questopiaApplication.getAudioPlayer();
    }

    public LiveData<String> getAudioErrorObserver() {
        return getAudioPlayer().getIsThrowError();
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences , key) -> {
        controllerObserver.postValue(getSettingsController());
        updatePageTemplate();
        refreshMainDesc();
        refreshVarsDesc();
        refreshActionsRecycler();
        refreshObjectsRecycler();
    };

    @SuppressLint("SetJavaScriptEnabled")
    public WebView getDefaultWebClient(WebView view) {
        var webViewClient = new GameWebViewClient();
        var webClientSettings = view.getSettings();
        webClientSettings.setAllowFileAccess(true);
        webClientSettings.setJavaScriptEnabled(true);
        webClientSettings.setUseWideViewPort(true);
        webClientSettings.setDomStorageEnabled(true);
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
            return convertRGBAtoBGRA(config.fontColor);
        } else {
            return getSettingsController().textColor;
        }
    }

    public int getBackgroundColor() {
        var config = getLibGameState().interfaceConfig;
        if (getSettingsController().isUseGameBackgroundColor && config.backColor != 0) {
            return convertRGBAtoBGRA(config.backColor);
        } else {
            return getSettingsController().backColor;
        }
    }

    public int getLinkColor() {
        var config = getLibGameState().interfaceConfig;
        if (getSettingsController().isUseGameLinkColor && config.linkColor != 0) {
            return convertRGBAtoBGRA(config.linkColor);
        } else {
            return getSettingsController().linkColor;
        }
    }

    public int getFontSize() {
        var config = getLibGameState().interfaceConfig;
        return getSettingsController().isUseGameFont && config.fontSize != 0 ?
                config.fontSize : getSettingsController().fontSize;
    }

    public String getHtml(String str) {
        var config = getLibGameState().interfaceConfig;
        return config.useHtml ?
                getHtmlProcessor().convertLibHtmlToWebHtml(str) :
                getHtmlProcessor().convertLibStrToHtml(str);
    }

    public Uri getImageUriFromPath(String src) {
        var relPath = Uri.parse(src).getPath();
        if (relPath == null) return Uri.EMPTY;
        if (getCurGameDir().isPresent()) {
            var imageFile = fromRelPath(getApplication(), relPath, getCurGameDir().get());
            return imageFile.getUri();
        } else {
            return Uri.EMPTY;
        }
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

    public Optional<DocumentFile> getCurGameDir() {
        if (gameDirUri == null) return Optional.empty();
        return Optional.ofNullable(DocumentFileCompat.fromUri(getApplication() , gameDirUri));
    }

    public Optional<DocumentFile> getSavesDir() {
        if (getCurGameDir().isEmpty()) return Optional.empty();
        var savesDir = findOrCreateFolder(getApplication(), getCurGameDir().get(), "saves");
        return Optional.ofNullable(savesDir);
    }

    @NonNull
    private GameActivity getGameActivity() {
        var activity = activityObserver.getValue();
        if (activity != null) {
            return activity;
        } else {
            throw new NullPointerException("Activity is null");
        }
    }

    public LibIConfig getIConfig() {
        return getLibGameState().interfaceConfig;
    }

    public void setGameDirUri(Uri gameDirUri) {
        this.gameDirUri = gameDirUri;
    }

    // endregion Getter/Setter

    public String removeHtmlTags(String dirtyHTML) {
        return getHtmlProcessor().removeHtmlTags(dirtyHTML);
    }

    private boolean isHasHTMLTags(String input) {
        return getHtmlProcessor().isContainsHtmlTags(input);
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        var optWindow = Optional.ofNullable(dialog.requireDialog().getWindow());
        if (optWindow.isEmpty()) return;

        if (dialog.getTag() != null) {
            switch (dialog.getTag()) {
                case "closeGameDialogFragment" -> {
                    stopAudio();
                    stopNativeLib();
                    removeCallback();
                    getGameActivity().finish();
                }
                case "inputDialogFragment" , "executorDialogFragment" -> {
                    var inputBoxEdit = (TextInputLayout) optWindow.get().findViewById(R.id.inputBox_edit);
                    var optInputBoxEditET = Optional.ofNullable(inputBoxEdit.getEditText());
                    if (optInputBoxEditET.isEmpty()) return;
                    var outputText = optInputBoxEditET.get().getText().toString();
                    if (Objects.equals(outputText , "")) {
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
                        Log.d(this.getClass().getSimpleName() , feedBackMessageET.getText().toString()
                                +"\n"+feedBackContactET.getText().toString()
                                +"\n"+feedBackNameET.getText().toString());
                    } else {
                        Log.d(this.getClass().getSimpleName() , feedBackContactET.getText().toString()
                                +"\n"+feedBackNameET.getText().toString());
                    }
                }
                case "loadGameDialogFragment" -> getGameActivity().startReadOrWriteSave(LOAD);
                case "showMessageDialogFragment" -> outputBooleanObserver.setValue(true);
            }
        }
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        if (dialog.getTag() != null) {
            if (dialog.getTag().equals("showMenuDialogFragment")) {
                outputIntObserver.setValue(-1);
            }
        }
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
        if (dialog.getTag() != null) {
            switch (dialog.getTag()) {
                case "inputDialogFragment" , "executorDialogFragment" ->
                        getGameActivity().getStorageHelper().openFilePicker("text/plain");
            }
        }
    }

    public void onDialogListClick(DialogFragment dialog , int which) {
        if (dialog.getTag() != null) {
            if (Objects.equals(dialog.getTag() , "showMenuDialogFragment")) {
                outputIntObserver.setValue(which);
            }
        }
    }

    public void updatePageTemplate() {
        var pageHeadTemplate = PAGE_HEAD_TEMPLATE
                .replace("QSPTEXTCOLOR" , getHexColor(getTextColor()))
                .replace("QSPBACKCOLOR" , getHexColor(getBackgroundColor()))
                .replace("QSPLINKCOLOR" , getHexColor(getLinkColor()))
                .replace("QSPFONTSTYLE" , getFontStyle(getSettingsController().getTypeface()))
                .replace("QSPFONTSIZE" , Integer.toString(getFontSize()));
        pageTemplate = pageHeadTemplate + PAGE_BODY_TEMPLATE;
    }

    private void refreshMainDesc() {
        CompletableFuture
                .supplyAsync(() -> getHtml(getLibGameState().mainDesc), service)
                .thenAcceptAsync(libMainDesc -> {
                    var dirtyHTML = pageTemplate.replace("REPLACETEXT", libMainDesc);
                    var cleanHTML = "";
                    if (getSettingsController().isImageDisabled) {
                        cleanHTML = getHtmlProcessor().getCleanHtmlRemMedia(dirtyHTML);
                    } else {
                        cleanHTML = getHtmlProcessor().getCleanHtmlAndMedia(getApplication() , dirtyHTML);
                    }
                    if (!cleanHTML.isBlank()) {
                        getGameActivity().warnUser(GameActivity.TAB_MAIN_DESC_AND_ACTIONS);
                    }
                    mainDescLiveData.postValue(cleanHTML);
                }, service);
    }

    private void refreshVarsDesc() {
        CompletableFuture
                .supplyAsync(() -> getHtml(getLibGameState().varsDesc), service)
                .thenAcceptAsync(libVarsDesc -> {
                    var dirtyHTML = pageTemplate.replace("REPLACETEXT" , libVarsDesc);
                    var cleanHTML = "";
                    if (getSettingsController().isImageDisabled) {
                        cleanHTML = getHtmlProcessor().getCleanHtmlRemMedia(dirtyHTML);
                    } else {
                        cleanHTML = getHtmlProcessor().getCleanHtmlAndMedia(getApplication() , dirtyHTML);
                    }
                    if (!cleanHTML.isBlank()) {
                        getGameActivity().warnUser(GameActivity.TAB_VARS_DESC);
                    }
                    varsDescLiveData.postValue(cleanHTML);
                }, service);
    }

    public void onActionClicked(int index) {
        getLibProxy().onActionClicked(index);
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
                }, service)
                .thenAcceptAsync(actionsRecycler -> {
                    actionsListLiveData.postValue(actionsRecycler);
                    int count = actionsRecycler.getItemCount();
                    isActionVisible.set(showActions && count > 0);
                }, service);
    }

    public void onObjectClicked(int index) {
        getLibProxy().onObjectSelected(index);
    }

    private void refreshObjectsRecycler() {
        CompletableFuture
                .supplyAsync(() -> {
                    getGameActivity().warnUser(GameActivity.TAB_OBJECTS);
                    var objectsRecycler = new GameItemRecycler();
                    objectsRecycler.setTypeface(getSettingsController().getTypeface());
                    objectsRecycler.setTextSize(getFontSize());
                    objectsRecycler.setTextColor(getTextColor());
                    objectsRecycler.setLinkTextColor(getLinkColor());
                    objectsRecycler.setBackgroundColor(getBackgroundColor());
                    objectsRecycler.submitList(getLibGameState().objectsList);
                    return objectsRecycler;
                }, service)
                .thenAcceptAsync(objectsListLiveData::postValue, service);
    }

    public void setCallback() {
        counterHandler.postDelayed(counterTask , counterInterval);
    }

    public void removeCallback() {
        counterHandler.removeCallbacks(counterTask);
    }

    public GameViewModel(@NonNull Application application) {
        super(application);
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        questopiaApplication = (QuestopiaApplication) getApplication();
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
        if (getCurGameDir().isEmpty()) return;
        getAudioPlayer().setCurGameDir(getCurGameDir().get());
        getAudioPlayer().setSoundEnabled(getSettingsController().isSoundEnabled);
        getAudioPlayer().resume();
    }

    public void stopAudio() {
        getAudioPlayer().stop();
    }

    public void startNativeLib() {
        getLibProxy().setGameInterface(this);
        getLibProxy().startLibThread();
    }

    public void stopNativeLib() {
        getLibProxy().stopLibThread();
        getLibProxy().setGameInterface(null);
    }

    public void runGameIntoNativeLib(String gameId ,
                                     String gameTitle ,
                                     DocumentFile gameDir ,
                                     DocumentFile gameFile) {
        getLibProxy().runGame(gameId, gameTitle, gameDir, gameFile);
    }

    public void requestForNativeLib(GameLibRequest req , Uri fileUri) {
        switch (req) {
            case LOAD_FILE -> doWithCounterDisabled(() ->
                    getLibProxy().loadGameState(fileUri));
            case SAVE_FILE -> getLibProxy().saveGameState(fileUri);
        }
    }

    public void requestForNativeLib(GameLibRequest req) {
        switch (req) {
            case USE_EXECUTOR -> getLibProxy().onUseExecutorString();
            case USE_INPUT -> getLibProxy().onInputAreaClicked();
            case RESTART_GAME -> getLibProxy().restartGame();
        }
    }

    public Boolean isGameRunning() {
        if (getLibProxy().getGameState() == null) return false;
        return getLibProxy().getGameState().gameRunning;
    }

    private int calculateMemoryCacheSize() {
        var activityManager = getApplication().getSystemService(ActivityManager.class);
        var memoryClass = activityManager.getLargeMemoryClass();
        // Target ~15% of the available heap.
        return 1024 * 1024 * memoryClass / 7;
    }

    private final TwoQCache<String, byte[]> cache = new TwoQCache<>(calculateMemoryCacheSize());

    public class GameWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view , WebResourceRequest request) {
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
                        getLibProxy().execute(removeHtmlTags(tempUriDecode));
                    } else {
                        getLibProxy().execute(tempUriDecode);
                    }
                }
                case "https" , "http" -> {
                    var viewLink = new Intent(Intent.ACTION_VIEW , Uri.parse(uriDecode));
                    viewLink.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplication().startActivity(viewLink);
                }
                case "file" -> {
                    try {
                        var tempLink = uri.getScheme().replace("file:/" , "https:");
                        var viewLazyLink = new Intent(Intent.ACTION_VIEW , Uri.parse(tempLink));
                        viewLazyLink.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(viewLazyLink);
                    } catch (ActivityNotFoundException e) {
                        showErrorDialog(e.getMessage() , ErrorType.EXCEPTION);
                    }
                }
            }

            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view ,
                                                          @NonNull WebResourceRequest request) {
            if (getCurGameDir().isEmpty()) return null;
            final var uri = request.getUrl();
            if (uri.getScheme() == null) return null;
            final var rootDir = getCurGameDir().get();

            if (!uri.getScheme().startsWith("file"))
                return super.shouldInterceptRequest(view , request);

            try {
                if (uri.getPath() == null) throw new NullPointerException();
                var imageFile = fromRelPath(getGameActivity(), uri.getPath(), rootDir);
                var extension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(imageFile));
                Log.d(TAG, cache.toString());
                if (cache.get(uri.getPath()) == null) {
                    var byteArray = getFileContents(getGameActivity(), imageFile.getUri());
                    cache.put(uri.getPath(), byteArray);
                    return new WebResourceResponse(extension, null, new ByteArrayInputStream(byteArray));
                } else {
                    return new WebResourceResponse(extension, null, new ByteArrayInputStream(cache.get(uri.getPath())));
                }
            } catch (NullPointerException ex) {
                if (getSettingsController().isUseImageDebug) {
                    showErrorDialog(uri.getPath() , ErrorType.IMAGE_ERROR);
                }
                return null;
            }
        }
    }

    // region GameInterface
    @Override
    public void refresh(final LibRefIRequest request) {
        if (request.isIConfigChanged) {
            getGameActivity().applySettings();
        }
        if (request.isIConfigChanged || request.isMainDescChanged) {
            updatePageTemplate();
            refreshMainDesc();
        }
        if (request.isActionsChanged) {
            refreshActionsRecycler();
        }
        if (request.isObjectsChanged) {
            refreshObjectsRecycler();
        }
        if (request.isIConfigChanged || request.isVarsDescChanged) {
            updatePageTemplate();
            refreshVarsDesc();
        }
    }

    @Override
    public void showErrorDialog(final String message) {
        getGameActivity().showSimpleDialog(message , GameDialogType.ERROR_DIALOG , null);
    }

    public void showErrorDialog(final String message , final ErrorType errorType) {
        getGameActivity().showSimpleDialog(message , GameDialogType.ERROR_DIALOG , errorType);
    }

    @Override
    public void showPicture(final String pathToImg) {
        getGameActivity().showSimpleDialog(pathToImg , GameDialogType.IMAGE_DIALOG , null);
    }

    @Override
    public void showMessage(final String message) {
        assertNonUiThread();

        final var latch = new CountDownLatch(1);
        getGameActivity().showMessageDialog(message , latch);
        try {
            latch.await();
        } catch (InterruptedException ex) {
            showErrorDialog(ex.getMessage() , ErrorType.WAITING_ERROR);
        }
    }

    @Override
    public String showInputDialog(final String prompt) {
        assertNonUiThread();

        final var inputQueue = new ArrayBlockingQueue<String>(1);
        getGameActivity().showInputDialog(prompt , inputQueue);
        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showErrorDialog(ex.getMessage() , ErrorType.WAITING_INPUT_ERROR);
            return "";
        }
    }

    @Override
    public String showExecutorDialog(final String text) {
        assertNonUiThread();

        final var inputQueue = new ArrayBlockingQueue<String>(1);
        getGameActivity().showExecutorDialog(text , inputQueue);
        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showErrorDialog(ex.getMessage() , ErrorType.WAITING_INPUT_ERROR);
            return "";
        }
    }

    @Override
    public int showMenu() {
        assertNonUiThread();

        final var resultQueue = new ArrayBlockingQueue<Integer>(1);
        final var currentItems = getLibProxy().getGameState().menuItemsList;
        final var newItems = new ArrayList<String>();
        currentItems.forEach(libMenuItem -> newItems.add(libMenuItem.name()));
        getGameActivity().showMenuDialog(newItems , resultQueue);
        try {
            return resultQueue.take();
        } catch (InterruptedException ex) {
            showErrorDialog(ex.getMessage() , ErrorType.WAITING_ERROR);
            return -1;
        }
    }

    @Override
    public void showLoadGamePopup() {
        getGameActivity().showSimpleDialog("" , GameDialogType.LOAD_DIALOG , null);
    }

    @Override
    public void showSaveGamePopup() {
        getGameActivity().showSavePopup();
    }

    @Override
    public void showWindow(LibWindowType type , final boolean show) {
        if (type == LibWindowType.ACTIONS) {
            showActions = show;
            refreshActionsRecycler();
        }
    }

    @Override
    public void setCounterInterval(int millis) {
        counterInterval = millis;
    }

    @Override
    public void doWithCounterDisabled(Runnable runnable) {
        counterHandler.removeCallbacks(counterTask);
        runnable.run();
        counterHandler.postDelayed(counterTask , counterInterval);
    }
    // endregion GameInterface
}
