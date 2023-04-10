package org.qp.android.viewModel;

import static org.qp.android.utils.Base64Util.decodeBase64;
import static org.qp.android.utils.Base64Util.hasBase64;
import static org.qp.android.utils.ColorUtil.convertRGBAToBGRA;
import static org.qp.android.utils.ColorUtil.getHexColor;
import static org.qp.android.utils.PathUtil.getExtension;
import static org.qp.android.utils.ThreadUtil.isMainThread;
import static org.qp.android.utils.ViewUtil.getFontStyle;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.dto.stock.GameData;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.QpMenuItem;
import org.qp.android.model.libQP.RefreshInterfaceRequest;
import org.qp.android.model.libQP.WindowType;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.game.GameInterface;
import org.qp.android.view.game.GameItemRecycler;
import org.qp.android.view.settings.SettingsController;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class GameViewModel extends AndroidViewModel implements GameInterface {
    private final String TAG = this.getClass().getSimpleName();

    private final QuestPlayerApplication questPlayerApplication;
    private final GameContentResolver gameContentResolver;
    private final HtmlProcessor htmlProcessor;
    private LibQpProxy libQpProxy;
    private AudioPlayer audioPlayer;

    public ObservableField<GameActivity> gameActivityObservableField =
            new ObservableField<>();
    public ObservableBoolean isActionVisible = new ObservableBoolean();

    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);

    private final MutableLiveData<SettingsController> controllerObserver = new MutableLiveData<>();
    private final MutableLiveData<String> mainDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> varsDescLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> actionLiveData = new MutableLiveData<>();
    private final MutableLiveData<GameItemRecycler> objectLiveData = new MutableLiveData<>();

    private static final String PAGE_HEAD_TEMPLATE = "<head>\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1\">\n"
            + "<style type=\"text/css\">\n"
            + "  body {\n"
            + "    margin: 0;\n"
            + "    padding: 0.5em;\n"
            + "    color: QSPTEXTCOLOR;\n"
            + "    background-color: QSPBACKCOLOR;\n"
            + "    font-size: QSPFONTSIZE;\n"
            + "    font-family: QSPFONTSTYLE;\n"
            + "  }\n"
            + "  a { color: QSPLINKCOLOR; }\n"
            + "  a:link { color: QSPLINKCOLOR; }\n"
            + "</style></head>";

    private static final String PAGE_BODY_TEMPLATE = "<body>REPLACETEXT</body>";
    public String pageTemplate = "";
    public SharedPreferences preferences;
    private boolean showActions = true;

    private final Handler counterHandler = new Handler();

    private int counterInterval = 500;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            libQpProxy.executeCounter();
            counterHandler.postDelayed(this, counterInterval);
        }
    };

    // region Getter/Setter
    public HtmlProcessor getHtmlProcessor() {
        htmlProcessor.setController(getSettingsController());
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQpProxy getLibQspProxy() {
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

    public WebViewClient getWebViewClient() {
        return new GameWebViewClient();
    }

    public LiveData<SettingsController> getControllerObserver() {
        return controllerObserver;
    }

    public int getTextColor() {
        var config = libQpProxy.getGameState().interfaceConfig;
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
                getHtmlProcessor().convertQspHtmlToWebViewHtml(str) :
                getHtmlProcessor().convertQspStringToWebViewHtml(str);
    }

    public String getImageAbsolutePath(String src) {
        var relPath = Uri.decode(src.substring(8));
        return gameContentResolver.getFile(relPath).getAbsolutePath();
    }

    public LiveData<String> getMainDescObserver () {
        return mainDescLiveData;
    }

    public LiveData<String> getVarsDescObserver () {
        return varsDescLiveData;
    }

    public LiveData<GameItemRecycler> getObjectsObserver () {
        if (objectLiveData.getValue() == null) {
            refreshObjectsRecycler();
        }
        return objectLiveData;
    }

    public LiveData<GameItemRecycler> getActionObserver () {
        if (actionLiveData.getValue() == null) {
            refreshActionsRecycler();
        }
        return actionLiveData;
    }

    @Nullable
    private GameActivity getGameActivity () {
        return gameActivityObservableField.get();
    }
    // endregion Getter/Setter

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
        var mainDesc = getHtml(getLibQspProxy().getGameState().mainDesc);
        mainDescLiveData.postValue(pageTemplate.replace("REPLACETEXT", mainDesc));
    }

    private void refreshVarsDesc() {
        var varsDesc = getHtml(getLibQspProxy().getGameState().varsDesc);
        varsDescLiveData.postValue(pageTemplate.replace("REPLACETEXT", varsDesc));
    }

    private void refreshActionsRecycler() {
        var actionsRecycler = new GameItemRecycler();
        actionsRecycler.setTypeface(getSettingsController().getTypeface());
        actionsRecycler.setTextSize(getFontSize());
        actionsRecycler.setTextColor(getTextColor());
        actionsRecycler.setLinkTextColor(getLinkColor());
        actionsRecycler.submitList(libQpProxy.getGameState().actions);
        actionLiveData.postValue(actionsRecycler);
        int count = actionsRecycler.getItemCount();
        isActionVisible.set(showActions && count > 0);
    }

    private void refreshObjectsRecycler() {
        var objectsRecycler = new GameItemRecycler();
        objectsRecycler.setTypeface(getSettingsController().getTypeface());
        objectsRecycler.setTextSize(getFontSize());
        objectsRecycler.setTextColor(getTextColor());
        objectsRecycler.setLinkTextColor(getLinkColor());
        objectsRecycler.submitList(libQpProxy.getGameState().objects);
        objectLiveData.postValue(objectsRecycler);
    }

    public void setCallback () {
        counterHandler.postDelayed(counterTask, counterInterval);
    }

    public void removeCallback() {
        counterHandler.removeCallbacks(counterTask);
    }

    public GameViewModel(@NonNull Application application) {
        super(application);
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        questPlayerApplication = getApplication();
        gameContentResolver = questPlayerApplication.getGameContentResolver();
        htmlProcessor = questPlayerApplication.getHtmlProcessor();
    }

    public void setGameSaveMap(Bundle gameSaveMap) {
        questPlayerApplication.setGameSaveMap(gameSaveMap);
    }

    public Bundle getGameSaveMap() {
        return questPlayerApplication.getGameSaveMap();
    }

    public ArrayList<GameData> getGameDataList() {
        return questPlayerApplication.getGameList();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    public void startAudio () {
        audioPlayer = questPlayerApplication.getAudioPlayer();
        audioPlayer.start();
    }

    public void stopAudio () {
        audioPlayer.stop();
        audioPlayer = null;
    }

    public void startLibQsp () {
        libQpProxy = questPlayerApplication.getLibQspProxy();
        libQpProxy.start();
    }

    public void stopLibQsp () {
        libQpProxy.stop();
        libQpProxy.setGameInterface(null);
        libQpProxy = null;
    }

    public class GameWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view ,
                                                @NonNull final String href) {
            var uriDecode = Uri.decode(href);
            if (href.toLowerCase().startsWith("exec:")) {
                var tempUriDecode = uriDecode.substring(5);
                if (hasBase64(tempUriDecode)) {
                    tempUriDecode = decodeBase64(uriDecode.substring(5));
                } else {
                    tempUriDecode = uriDecode.substring(5);
                }
                if (htmlProcessor.hasHTMLTags(tempUriDecode)) {
                    libQpProxy.execute(htmlProcessor.removeHTMLTags(tempUriDecode));
                } else {
                    libQpProxy.execute(tempUriDecode);
                }
            } else if (href.toLowerCase().startsWith("https:")
                    || href.toLowerCase().startsWith("http:")) {
                var intent = new Intent(Intent.ACTION_VIEW , Uri.parse(uriDecode));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            } else if (href.toLowerCase().startsWith("file:")) {
                var tempLink = href.replace("file:/" , "https:");
                var intent = new Intent(Intent.ACTION_VIEW , Uri.parse(tempLink));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            }
            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view ,
                                                          @NonNull WebResourceRequest request) {
            var uri = request.getUrl();
            if (uri.getScheme().startsWith("file")) {
                try {
                    var relPath = Uri.decode(uri.toString().substring(8));
                    var file = gameContentResolver.getFile(relPath);
                    var extension = getExtension(file.getName());
                    var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    var in = getApplication().getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType , null , in);
                } catch (FileNotFoundException | NullPointerException ex) {
                    Log.e(TAG , "File not found" , ex);
                    return null;
                }
            }
            return super.shouldInterceptRequest(view , request);
        }
    }

    // region GameInterface
    @Override
    public void refresh(final RefreshInterfaceRequest request) {
        if (request.interfaceConfigChanged) {
            if (getGameActivity() != null) {
                getGameActivity().applySettings();
            }
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
        if (getGameActivity() != null) {
            getGameActivity().showErrorDialog(message);
        }
    }

    @Override
    public void showPicture(final String pathToImg) {
        if (getGameActivity() != null) {
            getGameActivity().showPictureDialog(pathToImg);
        }
    }

    @Override
    public void showMessage(final String message) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final var latch = new CountDownLatch(1);
        if (getGameActivity() != null) {
            getGameActivity().showMessageDialog(message, latch);
        }
        try {
            latch.await();
        } catch (InterruptedException ex) {
            showError("Wait failed"+"\n"+ex);
        }
    }

    @Override
    public String showInputDialog(final String prompt) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);
        if (getGameActivity() != null) {
            getGameActivity().showInputDialog(prompt, inputQueue);
        }
        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait for input failed"+"\n"+ex);
            return "";
        }
    }

    @Override
    public String showExecutorDialog(final String text) {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final ArrayBlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(1);
        if (getGameActivity() != null) {
            getGameActivity().showExecutorDialog(text, inputQueue);
        }
        try {
            return inputQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait for input failed"+ex);
            return "";
        }
    }

    @Override
    public int showMenu() {
        if (isMainThread()) {
            throw new RuntimeException("Must not be called on the main thread");
        }
        final var resultQueue = new ArrayBlockingQueue<Integer>(1);
        final var items = new ArrayList<String>();
        for (QpMenuItem item : libQpProxy.getGameState().menuItems) {
            items.add(item.name);
        }
        if (getGameActivity() != null) {
            getGameActivity().showMenuDialog(items, resultQueue);
        }
        try {
            return resultQueue.take();
        } catch (InterruptedException ex) {
            showError("Wait failed"+"\n"+ex);
            return -1;
        }
    }

    @Override
    public void showLoadGamePopup() {
        if (getGameActivity() != null) {
            getGameActivity().showLoadDialog();
        }
    }

    @Override
    public void showSaveGamePopup(String filename) {
        if (getGameActivity() != null) {
            getGameActivity().showSavePopup();
        }
    }

    @Override
    public void showWindow(WindowType type, final boolean show) {
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
        counterHandler.postDelayed(counterTask, counterInterval);
    }

    // endregion GameInterface
}
