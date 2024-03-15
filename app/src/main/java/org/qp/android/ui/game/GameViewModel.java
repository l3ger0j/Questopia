package org.qp.android.ui.game;

import static org.qp.android.helpers.utils.Base64Util.decodeBase64;
import static org.qp.android.helpers.utils.Base64Util.isBase64;
import static org.qp.android.helpers.utils.ColorUtil.convertRGBAToBGRA;
import static org.qp.android.helpers.utils.ColorUtil.getHexColor;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.ThreadUtil.assertNonUiThread;
import static org.qp.android.helpers.utils.ViewUtil.getFontStyle;
import static org.qp.android.ui.game.GameActivity.LOAD;

import android.annotation.SuppressLint;
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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.helpers.ErrorType;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.RefreshInterfaceRequest;
import org.qp.android.model.libQP.WindowType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.dialogs.GameDialogType;
import org.qp.android.ui.settings.SettingsController;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class GameViewModel extends AndroidViewModel implements GameInterface {

    private final String TAG = this.getClass().getSimpleName();

    private final QuestPlayerApplication questPlayerApplication;
    private final HtmlProcessor htmlProcessor;
    private LibQpProxy libQpProxy;
    private AudioPlayer audioPlayer;
    private Uri gameDirUri;

    public ObservableBoolean isActionVisible = new ObservableBoolean();

    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);

    public MutableLiveData<GameActivity> activityObserver = new MutableLiveData<>();

    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> actionLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> objectLiveData = new MutableLiveData<>();

    private static final String PAGE_HEAD_TEMPLATE = """
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
            </style></head>""";

    private static final String PAGE_BODY_TEMPLATE = "<body>REPLACETEXT</body>";
    public String pageTemplate = "";
    public SharedPreferences preferences;
    private boolean showActions = true;

    private final Handler counterHandler = new Handler();

    private int counterInterval = 500;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            if (libQpProxy != null) {
                libQpProxy.executeCounter();
                counterHandler.postDelayed(this , counterInterval);
            }
        }
    };

    // region Getter/Setter
    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor.setController(getSettingsController());
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQpProxy getLibQspProxy() {
        if (libQpProxy == null) {
            libQpProxy = questPlayerApplication.getLibQspProxy();
        }

        libQpProxy.setGameInterface(this);
        return libQpProxy;
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
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setWebViewClient(webViewClient);
        return view;
    }

    public LiveData<SettingsController> getControllerObserver() {
        return controllerObserver;
    }

    public int getTextColor() {
        var libState = libQpProxy.getGameState();
        if (libState == null) return Color.WHITE;
        var config = libState.interfaceConfig;
        if (getSettingsController().isUseGameTextColor && config.fontColor != 0) {
            return convertRGBAToBGRA(config.fontColor);
        } else {
            return getSettingsController().textColor;
        }
    }

    public int getBackgroundColor() {
        var config = libQpProxy.getGameState().interfaceConfig;
        if (getSettingsController().isUseGameBackgroundColor && config.backColor != 0) {
            return convertRGBAToBGRA(config.backColor);
        } else {
            return getSettingsController().backColor;
        }
    }

    public int getLinkColor() {
        var config = libQpProxy.getGameState().interfaceConfig;
        if (getSettingsController().isUseGameLinkColor && config.linkColor != 0) {
            return convertRGBAToBGRA(config.linkColor);
        } else {
            return getSettingsController().linkColor;
        }
    }

    public int getFontSize() {
        var config = libQpProxy.getGameState().interfaceConfig;
        return getSettingsController().isUseGameFont && config.fontSize != 0 ?
                config.fontSize : getSettingsController().fontSize;
    }

    public String getHtml(String str) {
        var config = libQpProxy.getGameState().interfaceConfig;
        return config.useHtml ?
                getHtmlProcessor().convertLibHtmlToWebHtml(str) :
                getHtmlProcessor().convertQspStringToWebViewHtml(str);
    }

    public Uri getImageUri(String src) {
        var relPath = Uri.decode(src.substring(8));
        var app = (QuestPlayerApplication) getApplication();
        var curGameDir = app.getCurrentGameDir();
        var imageFile = fromRelPath(relPath , curGameDir);
        return imageFile.getUri();
    }

    public LiveData<String> getMainDescObserver() {
        return mainDescLiveData;
    }

    public LiveData<String> getVarsDescObserver() {
        return varsDescLiveData;
    }

    public LiveData<GameItemRecycler> getObjectsObserver() {
        if (objectLiveData.getValue() == null) {
            refreshObjectsRecycler();
        }
        return objectLiveData;
    }

    public LiveData<GameItemRecycler> getActionObserver() {
        if (actionLiveData.getValue() == null) {
            refreshActionsRecycler();
        }
        return actionLiveData;
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

    public void setGameDirUri(Uri gameDirUri) {
        this.gameDirUri = gameDirUri;
    }

    // endregion Getter/Setter

    public void onDialogPositiveClick(DialogFragment dialog) {
        var optWindow = Optional.ofNullable(dialog.requireDialog().getWindow());
        if (optWindow.isEmpty()) return;

        if (dialog.getTag() != null) {
            switch (dialog.getTag()) {
                case "closeGameDialogFragment" -> {
                    stopAudio();
                    stopLibQsp();
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
        var libMainDesc = getHtml(getLibQspProxy().getGameState().mainDesc);
        var dirtyHTML = pageTemplate.replace("REPLACETEXT" , libMainDesc);
        var cleanHTML = "";
        if (getSettingsController().isImageDisabled) {
            cleanHTML = getHtmlProcessor().getCleanHtmlPageNotImage(dirtyHTML);
        } else {
            cleanHTML = getHtmlProcessor().getCleanHtmlPageAndImage(dirtyHTML);
        }
        if (!cleanHTML.isBlank()) {
            getGameActivity().warnUser(GameActivity.TAB_MAIN_DESC_AND_ACTIONS);
        }
        mainDescLiveData.postValue(cleanHTML);
    }

    private void refreshVarsDesc() {
        var libVarsDesc = getHtml(getLibQspProxy().getGameState().varsDesc);
        var dirtyHTML = pageTemplate.replace("REPLACETEXT" , libVarsDesc);
        var cleanHTML = "";
        if (getSettingsController().isImageDisabled) {
            cleanHTML = getHtmlProcessor().getCleanHtmlPageNotImage(dirtyHTML);
        } else {
            cleanHTML = getHtmlProcessor().getCleanHtmlPageAndImage(dirtyHTML);
        }
        if (!cleanHTML.isBlank()) {
            getGameActivity().warnUser(GameActivity.TAB_VARS_DESC);
        }
        varsDescLiveData.postValue(cleanHTML);
    }

    private void refreshActionsRecycler() {
        var actionsRecycler = new GameItemRecycler();
        actionsRecycler.setTypeface(getSettingsController().getTypeface());
        actionsRecycler.setTextSize(getFontSize());
        actionsRecycler.setTextColor(getTextColor());
        actionsRecycler.setLinkTextColor(getLinkColor());
        actionsRecycler.setBackgroundColor(getBackgroundColor());
        actionsRecycler.submitList(libQpProxy.getGameState().actions);
        actionLiveData.postValue(actionsRecycler);
        int count = actionsRecycler.getItemCount();
        isActionVisible.set(showActions && count > 0);
    }

    private void refreshObjectsRecycler() {
        getGameActivity().warnUser(GameActivity.TAB_OBJECTS);
        var objectsRecycler = new GameItemRecycler();
        objectsRecycler.setTypeface(getSettingsController().getTypeface());
        objectsRecycler.setTextSize(getFontSize());
        objectsRecycler.setTextColor(getTextColor());
        objectsRecycler.setLinkTextColor(getLinkColor());
        objectsRecycler.setBackgroundColor(getBackgroundColor());
        objectsRecycler.submitList(libQpProxy.getGameState().objects);
        objectLiveData.postValue(objectsRecycler);
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
        questPlayerApplication = (QuestPlayerApplication) getApplication();
        htmlProcessor = questPlayerApplication.getHtmlProcessor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public void startAudio() {
        audioPlayer = questPlayerApplication.getAudioPlayer();
        audioPlayer.start(getApplication());
    }

    public void stopAudio() {
        audioPlayer.stop();
        audioPlayer = null;
    }

    public void startLibQsp() {
        libQpProxy = questPlayerApplication.getLibQspProxy();
        libQpProxy.start();
    }

    public void stopLibQsp() {
        libQpProxy.stop();
        libQpProxy.setGameInterface(null);
        libQpProxy = null;
    }

    public class GameWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view , WebResourceRequest request) {
            final var uri = request.getUrl();
            final var uriDecode = Uri.decode(uri.toString());
            switch (uri.getScheme()) {
                case "exec" -> {
                    var tempUriDecode = uriDecode.substring(5);
                    if (isBase64(tempUriDecode)) {
                        tempUriDecode = decodeBase64(uriDecode.substring(5));
                    } else {
                        tempUriDecode = uriDecode.substring(5);
                    }
                    if (htmlProcessor.hasHTMLTags(tempUriDecode)) {
                        libQpProxy.execute(htmlProcessor.removeHTMLTags(tempUriDecode));
                    } else {
                        libQpProxy.execute(tempUriDecode);
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
            var uri = request.getUrl();
            var rootDir = DocumentFileCompat.fromUri(getApplication() , gameDirUri);
            if (rootDir != null && uri.getScheme() != null) {
                if (uri.getScheme().startsWith("file")) {
                    try {
                        if (uri.getPath() == null) throw new NullPointerException();
                        var imageFile = fromRelPath(uri.getPath() , rootDir);
                        var extension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(documentWrap(imageFile).getExtension());
                        var in = getApplication().getContentResolver().openInputStream(imageFile.getUri());
                        return new WebResourceResponse(extension , "utf-8" , in);
                    } catch (FileNotFoundException | NullPointerException ex) {
                        if (getSettingsController().isUseImageDebug) {
                            showErrorDialog(uri.getPath() , ErrorType.IMAGE_ERROR);
                        }
                        return null;
                    }
                }
                return super.shouldInterceptRequest(view , request);
            }
            return null;
        }
    }

    // region GameInterface
    @Override
    public void refresh(final RefreshInterfaceRequest request) {
        if (request.interfaceConfigChanged) {
            getGameActivity().applySettings();
        }
        if (request.interfaceConfigChanged || request.mainDescChanged) {
            updatePageTemplate();
            refreshMainDesc();
        }
        if (request.actionsChanged) {
            refreshActionsRecycler();
        }
        if (request.objectsChanged) {
            refreshObjectsRecycler();
        }
        if (request.interfaceConfigChanged || request.varsDescChanged) {
            updatePageTemplate();
            refreshVarsDesc();
        }
    }

    @Override
    public void showError(final String message) {
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
        final var currentItems = libQpProxy.getGameState().menuItems;
        final var newItems = new ArrayList<String>();
        for (var item : currentItems) {
            newItems.add(item.name);
        }
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
    public void showWindow(WindowType type , final boolean show) {
        if (type == WindowType.ACTIONS) {
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
