package com.qsp.player.view.adapters;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

public class BindAdapters {

    @BindingAdapter({"setWebViewClient"})
    public static void setWebViewClient(@NonNull WebView view, WebViewClient client) {
        WebSettings webViewSettings = view.getSettings();
        webViewSettings.setAllowFileAccess(true);
        view.setWebViewClient(client);
    }
}
