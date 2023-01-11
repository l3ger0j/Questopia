package org.qp.android.viewModel.viewModels;

import static org.qp.android.utils.Base64Util.decodeBase64;
import static org.qp.android.utils.Base64Util.hasBase64;
import static org.qp.android.utils.ColorUtil.convertRGBAToBGRA;
import static org.qp.android.utils.LanguageUtil.setLocale;
import static org.qp.android.utils.PathUtil.getExtension;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.model.libQSP.LibQspProxy;
import org.qp.android.model.libQSP.QpListItem;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.game.GameInterface;
import org.qp.android.view.settings.SettingsController;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ActivityGame extends AndroidViewModel {
    private final String TAG = this.getClass().getCanonicalName();

    private final QuestPlayerApplication questPlayerApplication;
    private final GameContentResolver gameContentResolver;
    private final SettingsController settingsController;
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

    public SettingsController getSettingsController() {
        return settingsController;
    }

    public WebViewClient getWebViewClient() {
        return new GameWebViewClient();
    }

    public GameItemAdapter getQspItemAdapter (Context context,
                                              int resource,
                                              ArrayList<QpListItem> items) {
        return new GameItemAdapter(context, resource, items);
    }

    public int getTextColor() {
        var config = libQspProxy.getGameState().interfaceConfig;
        return config.fontColor != 0 ?
                convertRGBAToBGRA(config.fontColor) : settingsController.textColor;
    }

    public int getBackgroundColor() {
        var config = libQspProxy.getGameState().interfaceConfig;
        return settingsController.backColor != 0 ?
                settingsController.backColor : convertRGBAToBGRA(config.backColor);
    }

    public int getLinkColor() {
        var config = libQspProxy.getGameState().interfaceConfig;
        return config.linkColor != 0 ?
                convertRGBAToBGRA(config.linkColor) : settingsController.linkColor;
    }

    public int getFontSize() {
        var config = libQspProxy.getGameState().interfaceConfig;
        return settingsController.isUseGameFont && config.fontSize != 0 ?
                config.fontSize : settingsController.fontSize;
    }
    // endregion Getter/Setter

    public ActivityGame(@NonNull Application application) {
        super(application);
        questPlayerApplication = getApplication();
        gameContentResolver = questPlayerApplication.getGameContentResolver();
        htmlProcessor = questPlayerApplication.getHtmlProcessor();
        settingsController = SettingsController.newInstance().loadSettings(getApplication());
    }

    public String loadLocale(Context context, SettingsController settingsController) {
        setLocale(context, settingsController.language);
        return settingsController.language;
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

    public class GameWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                @NonNull final String href) {
            var uriDecode = Uri.decode(href);
            if (href.toLowerCase().startsWith("exec:")) {
                var tempUriDecode = uriDecode.substring(5);
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
                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriDecode));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            } else if (href.toLowerCase().startsWith("file:")) {
                var tempLink = href.replace("file:/", "https:");
                Log.d(TAG, tempLink);
                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tempLink));
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
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException | NullPointerException ex) {
                    Log.e(TAG,"File not found" , ex);
                    return null;
                }
            }
            return super.shouldInterceptRequest(view , request);
        }
    }

    public class GameItemAdapter extends ArrayAdapter<QpListItem> {
        private final int resource;
        private final ArrayList<QpListItem> items;

        GameItemAdapter(Context context, int resource, ArrayList<QpListItem> items) {
            super(context, resource, items);
            this.resource = resource;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                var inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(resource, null);
            }
            var item = items.get(position);
            if (item != null) {
                TextView textView = convertView.findViewById(R.id.item_text);
                textView.setCompoundDrawablesWithIntrinsicBounds(item.icon,
                        null, null, null);
                textView.setTypeface(getTypeface());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getFontSize());
                textView.setBackgroundColor(getBackgroundColor());
                textView.setTextColor(getTextColor());
                textView.setLinkTextColor(getLinkColor());
                textView.setText(item.text);
            }

            return convertView;
        }

        private Typeface getTypeface() {
            switch (settingsController.typeface) {
                case 1:
                    return Typeface.SANS_SERIF;
                case 2:
                    return Typeface.SERIF;
                case 3:
                    return Typeface.MONOSPACE;
                default:
                    return Typeface.DEFAULT;
            }
        }
    }
}
