package org.qp.android.ui.settings;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.ui.dialogs.SettingsDialogFrag;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);

        var viewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        var versionPref = findPreference("showVersion");
        if (versionPref != null) {
            versionPref.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
            versionPref.setSummaryProvider(preference -> {
                var application = (QuestPlayerApplication) requireActivity().getApplication();
                var libQspProxy = application.getLibQspProxy();
                try {
                    var compileDateTime = libQspProxy.getCompiledDateTime();
                    var versionQSP = libQspProxy.getVersionQSP();
                    return compileDateTime+"\n"+versionQSP;
                } catch (NullPointerException ex) {
                    return null;
                }
            });
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
                case "showExtensionMenu" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.pluginFragment);
                    return true;
                }
                case "newsApp" -> {
                    Navigation.findNavController(requireView()).navigate(R.id.newsFragment);
                    return true;
                }
                case "showAbout" -> {
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
                    webView.loadDataWithBaseURL(
                            "",
                            viewModel.formationAboutDesc(requireContext()),
                            "text/html",
                            "utf-8",
                            "");
                    webView.setLayoutParams(lpView);
                    linearLayout.addView(webView);
                    var dialogFrag = new SettingsDialogFrag();
                    dialogFrag.setView(linearLayout);
                    dialogFrag.show(getParentFragmentManager(), "settingsDialogFragment");
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
}