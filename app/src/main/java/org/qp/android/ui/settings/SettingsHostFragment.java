package org.qp.android.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.webkit.WebViewAssetLoader;

import org.qp.android.BuildConfig;
import org.qp.android.R;
import org.qp.android.ui.dialogs.SettingsDialogFrag;

import java.util.Optional;

public class SettingsHostFragment extends PreferenceFragmentCompat {

    private SettingsViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState , String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        viewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        var versionPref = findPreference("showVersion");
        if (versionPref != null) {
            versionPref.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-" , BuildConfig.VERSION_NAME));
            versionPref.setSummaryProvider(preference ->
                    "Lib version: "+"5.7.0" + "\nTimestamp: " + BuildConfig.BUILD_TIME
            );
        }

        Preference.OnPreferenceClickListener listener = preference -> {
            switch (preference.getKey()) {
                case "generalPref" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.settingGeneralFragment);
                    return true;
                }
                case "textPref" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.settingTextFragment);
                    return true;
                }
                case "imagePref" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.settingImageFragment);
                    return true;
                }
                case "soundPref" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.settingSoundFragment);
                    return true;
                }
                case "newsApp" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.newsFragment);
                    return true;
                }
                case "showAbout" -> {
                    createCustomView();
                    return true;
                }
            }
            return false;
        };

        var generalPref = findPreference("generalPref");
        if (generalPref != null)
            generalPref.setOnPreferenceClickListener(listener);

        var textPref = findPreference("textPref");
        if (textPref != null)
            textPref.setOnPreferenceClickListener(listener);

        var imagePref = findPreference("imagePref");
        if (imagePref != null)
            imagePref.setOnPreferenceClickListener(listener);

        var soundPref = findPreference("soundPref");
        if (soundPref != null)
            soundPref.setOnPreferenceClickListener(listener);

        var pluginPref = findPreference("showExtensionMenu");
        if (pluginPref != null)
            pluginPref.setOnPreferenceClickListener(listener);

        var newsPref = findPreference("newsApp");
        if (newsPref != null)
            newsPref.setOnPreferenceClickListener(listener);

        var aboutPref = findPreference("showAbout");
        if (aboutPref != null)
            aboutPref.setOnPreferenceClickListener(listener);
    }

    private void createCustomView() {
        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                .addPathHandler("/res/", new WebViewAssetLoader.ResourcesPathHandler(requireContext()))
                .build();

        var linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        var linLayoutParam =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT ,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(linLayoutParam);
        var lpView =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT ,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        var webView = new WebView(requireContext());
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view ,
                                                              WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view , WebResourceRequest request) {
                var url = Optional.ofNullable(request.getUrl());
                if (url.isPresent()) {
                    var workUri = url.get();
                    if (workUri.getScheme().startsWith("http")
                            || workUri.getScheme().startsWith("https")) {
                        requireContext().startActivity(new Intent(Intent.ACTION_VIEW, workUri));
                        return true;
                    }
                }
                return false;
            }
        });
        webView.loadDataWithBaseURL(
                null ,
                viewModel.formationAboutDesc(requireContext()) ,
                null ,
                "utf-8" ,
                null);
        webView.setLayoutParams(lpView);
        linearLayout.addView(webView);
        var dialogFrag = new SettingsDialogFrag();
        dialogFrag.setView(linearLayout);
        dialogFrag.show(getParentFragmentManager() , "aboutDialogFragment");
    }
}