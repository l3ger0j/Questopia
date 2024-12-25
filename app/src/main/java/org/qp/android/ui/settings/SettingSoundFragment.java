package org.qp.android.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.R;

public class SettingSoundFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState ,
                                    @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.setting_sound);
    }
}
