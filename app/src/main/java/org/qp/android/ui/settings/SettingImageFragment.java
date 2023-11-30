package org.qp.android.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.R;

public class SettingImageFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState ,
                                    @Nullable String rootKey) {
        requireActivity().setTitle(R.string.imageCatTitle);
        addPreferencesFromResource(R.xml.setting_image);

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
    }
}
