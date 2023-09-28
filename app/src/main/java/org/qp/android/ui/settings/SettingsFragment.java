package org.qp.android.ui.settings;

import android.content.SharedPreferences;
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
import org.qp.android.helpers.utils.ViewUtil;
import org.qp.android.ui.dialogs.SettingsDialogFrag;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener  {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);

        var viewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        var customWidthImagePref = findPreference("customWidth");
        if (customWidthImagePref != null) {
            customWidthImagePref.setEnabled(!viewModel.getSettingsController().isUseAutoWidth);
        }
        var customHeightImagePref = findPreference("customHeight");
        if (customHeightImagePref != null) {
            customHeightImagePref.setEnabled(!viewModel.getSettingsController().isUseAutoHeight);
        }

        var textColorPref = findPreference("textColor");
        if (textColorPref != null) {
            textColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#000000"));
            textColorPref.setEnabled(!viewModel.getSettingsController().isUseGameTextColor);
        }
        var backColorPref = findPreference("backColor");
        if (backColorPref != null) {
            backColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#e0e0e0"));
            backColorPref.setEnabled(!viewModel.getSettingsController().isUseGameBackgroundColor);
        }
        var linkColorPref = findPreference("linkColor");
        if (linkColorPref != null) {
            linkColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#0000ff"));
            linkColorPref.setEnabled(!viewModel.getSettingsController().isUseGameLinkColor);
        }
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
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Objects.equals(key , "lang")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.closeToApply));
        } else if (key.equals("binPref")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.settingsEffect));
        }
    }
}