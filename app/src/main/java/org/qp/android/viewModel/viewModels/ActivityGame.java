package org.qp.android.viewModel.viewModels;

import static org.qp.android.utils.Base64Util.decodeBase64;
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

import org.qp.android.model.libQSP.LibQspProxy;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.settings.SettingsController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ActivityGame extends AndroidViewModel {
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

    public ActivityGame(@NonNull Application application) {
        super(application);
    }

    public class QspWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                @NonNull final String href) {
            String uriDecode = Uri.decode(href);
            if (href.toLowerCase().startsWith("exec:")) {
                Log.d(TAG, decodeBase64(uriDecode));
                try {
                    libQspProxy.execute(decodeBase64(uriDecode.substring(5)));
                } catch (IllegalArgumentException exception) {
                    libQspProxy.execute(uriDecode.substring(5));
                }
            } else if (href.toLowerCase().startsWith("https:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriDecode));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
            } else if (href.toLowerCase().startsWith("http:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriDecode));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(intent);
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
