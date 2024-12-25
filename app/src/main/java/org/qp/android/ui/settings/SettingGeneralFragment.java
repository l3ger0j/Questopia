package org.qp.android.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.R;
import org.qp.android.helpers.utils.ViewUtil;

import java.util.Objects;

public class SettingGeneralFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState ,
                                    @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.setting_general);
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
        if (Objects.equals("binPref", key)) {
            ViewUtil.showSnackBar(getView(), getString(R.string.settingsEffect));
        }
    }
}
