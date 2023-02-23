package org.qp.android.view.adapters;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import org.qp.android.R;

public class Bind {
    @BindingAdapter({"setWebViewClient"})
    public static void setWebViewClient(WebView view, WebViewClient client) {
        var webViewSettings = view.getSettings();
        webViewSettings.setAllowFileAccess(true);
        webViewSettings.setDomStorageEnabled(true);
        view.setWebViewClient(client);
    }

    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .into(view);
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources() ,
                    R.drawable.broken_image , null
            );
            view.setImageDrawable(drawable);
        }
    }
}
