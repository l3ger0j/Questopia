package org.qp.android.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.qp.android.R;
import org.qp.android.helpers.utils.ViewUtil;

import java.util.Objects;

public class SettingGeneralFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState ,
                                    @Nullable String rootKey) {
        requireActivity().setTitle(R.string.generalCatTitle);
        addPreferencesFromResource(R.xml.setting_general);

        var prefVizSep = findPreference("separator");
        if (prefVizSep != null) {
            var switchPrefVizSep = (SwitchPreferenceCompat) prefVizSep;
            switchPrefVizSep.setOnPreferenceClickListener(preference -> {
                if (switchPrefVizSep.isChecked()) {
                    preference.setIcon(R.drawable.baseline_visibility_off_24);
                } else {
                    preference.setIcon(R.drawable.baseline_visibility_24);
                }
                return true;
            });
        }
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
