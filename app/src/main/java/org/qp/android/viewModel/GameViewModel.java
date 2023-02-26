package org.qp.android.viewModel;

import static org.qp.android.utils.Base64Util.decodeBase64;
import static org.qp.android.utils.Base64Util.hasBase64;
import static org.qp.android.utils.ColorUtil.convertRGBAToBGRA;
import static org.qp.android.utils.ColorUtil.getHexColor;
import static org.qp.android.utils.PathUtil.getExtension;
import static org.qp.android.utils.ViewUtil.getFontStyle;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.game.GameInterface;
import org.qp.android.view.settings.SettingsController;

import java.io.FileNotFoundException;

public class GameViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getCanonicalName();

    private final QuestPlayerApplication questPlayerApplication;
    private final GameContentResolver gameContentResolver;
    private final HtmlProcessor htmlProcessor;
    private LibQpProxy libQpProxy;
    private AudioPlayer audioPlayer;

    public ObservableField<GameActivity> gameActivityObservableField =
            new ObservableField<>();

    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);

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

    // region Getter/Setter
    public HtmlProcessor getHtmlProcessor() {
        htmlProcessor.setController(getSettingsController());
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQpProxy getLibQspProxy() {
        return libQpProxy;
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance().loadSettings(getApplication());
    }

    public WebViewClient getWebViewClient() {
        return new GameWebViewClient();
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

    public String getMainDesc () {
        var mainDesc = getHtml(libQpProxy.getGameState().mainDesc);
        return pageTemplate.replace("REPLACETEXT", mainDesc);
    }

    public String getVarsDesc () {
        var varsDesc = getHtml(libQpProxy.getGameState().varsDesc);
        return pageTemplate.replace("REPLACETEXT", varsDesc);
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

    public GameViewModel(@NonNull Application application) {
        super(application);
        questPlayerApplication = getApplication();
        gameContentResolver = questPlayerApplication.getGameContentResolver();
        htmlProcessor = questPlayerApplication.getHtmlProcessor();
    }

    public void startAudio () {
        audioPlayer = questPlayerApplication.getAudioPlayer();
        audioPlayer.start();
    }

    public void stopAudio () {
        audioPlayer.stop();
        audioPlayer = null;
    }

    public void startLibQsp (GameInterface gameInterface) {
        libQpProxy = questPlayerApplication.getLibQspProxy();
        libQpProxy.setGameInterface(gameInterface);
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
}
