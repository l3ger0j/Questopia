package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.Base64Util.decodeBase64;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.PathUtil.getExtension;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
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

import com.qsp.player.model.libQSP.LibQspProxy;
import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.view.activities.GameActivity;
import com.qsp.player.view.adapters.SettingsAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class GameActivityVM extends AndroidViewModel {
    private final String TAG = this.getClass().getCanonicalName();

    private GameContentResolver gameContentResolver;
    private LibQspProxy libQspProxy;

    public ObservableField<GameActivity> gameActivityObservableField =
            new ObservableField<>();

    // region Getter/Setter
    public void setLibQspProxy(LibQspProxy libQspProxy) {
        this.libQspProxy = libQspProxy;
    }

    public void setGameContentResolver(GameContentResolver gameContentResolver) {
        this.gameContentResolver = gameContentResolver;
    }

    public WebViewClient getWebViewClient() {
        return new QspWebViewClient();
    }
    // endregion Getter/Setter

    public GameActivityVM(@NonNull Application application) {
        super(application);
    }

    // region Dialog
    public void showPictureDialog (String pathToImage) {
        Objects.requireNonNull(gameActivityObservableField.get()).onShowDialog(pathToImage);
    }
    // endregion Dialog

    public class QspWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                @NonNull final String href) {
            String uriDecode = Uri.decode(href);
            if (href.toLowerCase().startsWith("exec:")) {
                try {
                    libQspProxy.execute(decodeBase64(uriDecode.substring(5)));
                } catch (IllegalArgumentException exception) {
                    libQspProxy.execute(uriDecode.substring(5));
                }
            }
            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view , WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri.getScheme().startsWith("file")) {
                try {
                    String relPath = Uri.decode(uri.toString().substring(8));
                    File file = gameContentResolver.getFile(relPath);
                    String extension = getExtension(file.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    InputStream in = getApplication().getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException ex) {
                    Log.e(TAG,"File not found" , ex);
                    return null;
                }
            }
            return super.shouldInterceptRequest(view , request);
        }
    }

    public SettingsAdapter loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return SettingsAdapter.from(preferences);
    }

    public String loadLocale(Context context, SettingsAdapter settingsAdapter) {
        setLocale(context, settingsAdapter.language);
        return settingsAdapter.language;
    }

}
