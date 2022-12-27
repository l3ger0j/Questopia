package org.qp.android.viewModel.viewModels;

import static org.qp.android.utils.Base64Util.decodeBase64;
import static org.qp.android.utils.Base64Util.hasBase64;
import static org.qp.android.utils.LanguageUtil.setLocale;
import static org.qp.android.utils.PathUtil.getExtension;

import android.app.Application;
import android.content.Context;
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
import org.qp.android.model.libQSP.LibQspProxy;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.game.GameInterface;
import org.qp.android.view.settings.SettingsController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ActivityGame extends AndroidViewModel {
    private final String TAG = this.getClass().getCanonicalName();

    private final QuestPlayerApplication questPlayerApplication;
    private final GameContentResolver gameContentResolver;
    private final HtmlProcessor htmlProcessor;
    private LibQspProxy libQspProxy;
    private AudioPlayer audioPlayer;

    public ObservableField<GameActivity> gameActivityObservableField =
            new ObservableField<>();

    public MutableLiveData<String> outputTextObserver = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> outputBooleanObserver = new MutableLiveData<>(false);

    // region Getter/Setter
    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
    }

    public WebViewClient getWebViewClient() {
        return new QspWebViewClient();
    }
    // endregion Getter/Setter

    public ActivityGame(@NonNull Application application) {
        super(application);
        questPlayerApplication = getApplication();
        gameContentResolver = questPlayerApplication.getGameContentResolver();
        htmlProcessor = questPlayerApplication.getHtmlProcessor();
    }

    public AudioPlayer startAudio () {
        audioPlayer = questPlayerApplication.getAudioPlayer();
        audioPlayer.start();
        return audioPlayer;
    }

    public void stopAudio () {
        audioPlayer.stop();
        audioPlayer = null;
    }

    public LibQspProxy startLibQsp (GameInterface gameInterface) {
        libQspProxy = questPlayerApplication.getLibQspProxy();
        libQspProxy.setGameInterface(gameInterface);
        libQspProxy.start();
        return libQspProxy;
    }

    public void stopLibQsp () {
        libQspProxy.stop();
        libQspProxy.setGameInterface(null);
        libQspProxy = null;
    }

    public class QspWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                @NonNull final String href) {
            String uriDecode = Uri.decode(href);
            if (href.toLowerCase().startsWith("exec:")) {
                String tempUriDecode = uriDecode.substring(5);
                Log.d(TAG, String.valueOf(hasBase64(tempUriDecode)));
                if (hasBase64(tempUriDecode)) {
                    tempUriDecode = decodeBase64(uriDecode.substring(5));
                } else {
                    tempUriDecode = uriDecode.substring(5);
                }
                if (htmlProcessor.hasHTMLTags(tempUriDecode)) {
                    libQspProxy.execute(htmlProcessor.removeHTMLTags(tempUriDecode));
                } else {
                    libQspProxy.execute(tempUriDecode);
                }
            } else if (href.toLowerCase().startsWith("https:")
                    || href.toLowerCase().startsWith("http:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriDecode));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            } else if (href.toLowerCase().startsWith("file:")) {
                String tempLink = href.replace("file:/", "https:");
                Log.d(TAG, tempLink);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tempLink));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            }
            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view ,
                                                          @NonNull WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri.getScheme().startsWith("file")) {
                try {
                    String relPath = Uri.decode(uri.toString().substring(8));
                    File file = gameContentResolver.getFile(relPath);
                    String extension = getExtension(file.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    InputStream in = getApplication().getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException | NullPointerException ex) {
                    Log.e(TAG,"File not found" , ex);
                    return null;
                }
            }
            return super.shouldInterceptRequest(view , request);
        }
    }

    public String loadLocale(Context context, SettingsController settingsController) {
        setLocale(context, settingsController.language);
        return settingsController.language;
    }

}
