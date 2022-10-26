package org.qp.android.view.adapters;

import android.graphics.drawable.Drawable;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import org.qp.android.R;

import java.io.File;

public class Bind {

    @BindingAdapter({"setWebViewClient"})
    public static void setWebViewClient(@NonNull WebView view, WebViewClient client) {
        WebSettings webViewSettings = view.getSettings();
        webViewSettings.setAllowFileAccess(true);
        view.setWebViewClient(client);
    }

    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        if (imageUrl.isEmpty()) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources(),
                    R.drawable.broken_image , null
            );
            view.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(new File(imageUrl))
                    .into(view);
        }
    }
}
