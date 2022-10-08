package com.qsp.player.view.adapters;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.qsp.player.R;
import com.qsp.player.model.service.ImageProvider;
import com.squareup.picasso.Picasso;

import java.io.File;

public class BindAdapters {

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
                    .error(new ImageProvider().getOld(imageUrl))
                    .into(view);
        }
    }
}
