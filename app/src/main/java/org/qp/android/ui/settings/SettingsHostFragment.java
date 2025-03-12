package org.qp.android.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.BuildConfig;
import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.ui.dialogs.SettingsDialogFrag;

public class SettingsHostFragment extends PreferenceFragmentCompat {

    private static final int INITIAL_SCALE = 294;
    private SettingsViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        var versionPref = findPreference("showVersion");
        if (versionPref != null) {
            var application = (QuestopiaApplication) requireActivity().getApplication();
            versionPref.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
//            if (application.getCurrPluginClient() != null) {
//                versionPref.setSummaryProvider(preference ->
//                        "Lib version: " + "5.7.0" + "\nTimestamp: " + BuildConfig.BUILD_TIME
//                );
//            }
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
                case "showExtensionMenu" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.pluginFragment);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var rootIns = ViewCompat.getRootWindowInsets(view);
        if (rootIns == null) return;
        var ins = rootIns.getInsets(
                WindowInsetsCompat.Type.systemBars() |
                        WindowInsetsCompat.Type.displayCutout()
        );

        view.setPadding(
                view.getPaddingStart() + ins.left,
                view.getPaddingTop(),
                view.getPaddingRight() + ins.right,
                view.getPaddingBottom() + ins.bottom
        );
    }

    private void createCustomView() {
        var linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        var linLayoutParam =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(linLayoutParam);
        var lpView =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        var webView = new WebView(requireContext());
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl() == null) return false;
                var workUri = request.getUrl();
                if (workUri.getScheme() == null) return false;
                var schemeUri = workUri.getScheme();

                if (schemeUri.startsWith("http") || schemeUri.startsWith("https")) {
                    requireContext().startActivity(new Intent(Intent.ACTION_VIEW, workUri));
                    return true;
                }

                return false;
            }
        });
        webView.loadUrl(viewModel.getLinkAboutDesc());
        webView.setLayoutParams(lpView);
        linearLayout.addView(webView);
        var dialogFrag = new SettingsDialogFrag();
        dialogFrag.setView(linearLayout);
        dialogFrag.show(getParentFragmentManager(), "aboutDialogFragment");
    }
}