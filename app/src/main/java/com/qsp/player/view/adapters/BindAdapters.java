package com.qsp.player.view.adapters;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

import com.qsp.player.R;
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
        Picasso.get()
                .load(new File(imageUrl))
                .error(R.drawable.broken_image)
                .resize(getScreenWidth() - 200, getScreenHeight() - 1000)
                .into(view);
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
}
