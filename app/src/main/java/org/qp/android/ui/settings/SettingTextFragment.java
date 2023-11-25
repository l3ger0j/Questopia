package org.qp.android.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.R;

public class SettingTextFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState ,
                                    @Nullable String rootKey) {
        requireActivity().setTitle(R.string.textCatTitle);
        addPreferencesFromResource(R.xml.setting_text);

        var viewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

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
    }
}
