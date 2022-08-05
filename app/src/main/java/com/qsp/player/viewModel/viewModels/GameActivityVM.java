package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.Base64Util.decodeBase64;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.PathUtil.getExtension;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;

import com.qsp.player.model.libQSP.LibQspProxy;
import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.view.adapters.SettingsAdapter;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GameActivityVM extends AndroidViewModel {

    private GameContentResolver gameContentResolver;
    private LibQspProxy libQspProxy;
    private Logger logger;

    public GameActivityVM(@NonNull Application application) {
        super(application);
    }

    public void setLibQspProxy(LibQspProxy libQspProxy) {
        this.libQspProxy = libQspProxy;
    }

    public void setGameContentResolver(GameContentResolver gameContentResolver) {
        this.gameContentResolver = gameContentResolver;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public WebViewClient getWebViewClient() {
        return new QspWebViewClient();
    }

    public class QspWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                @NonNull final String href) {
            if (href.toLowerCase().startsWith("exec:")) {
                String code = decodeBase64(href.substring(5));
                libQspProxy.execute(code);
            }
            return true;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view , WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri.getScheme().startsWith("file")) {
                try {
                    File file = gameContentResolver.getFile(uri.toString().substring(8));
                    String extension = getExtension(file.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    InputStream in = getApplication().getContentResolver().openInputStream(Uri.fromFile(file));
                    return new WebResourceResponse(mimeType, null, in);
                } catch (FileNotFoundException ex) {
                    logger.error("File not found", ex);
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
