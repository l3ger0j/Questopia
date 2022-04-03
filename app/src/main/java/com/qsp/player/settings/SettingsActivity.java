package com.qsp.player.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kizitonwose.colorpreferencecompat.ColorPreferenceCompat;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;
import com.qsp.player.R;
import com.qsp.player.shared.util.ViewUtil;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            requireActivity().setTitle(R.string.settings);
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onResume() {
            super.onResume();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
            switch (key) {
                case "backColor":
                case "textColor":
                case "linkColor":
                    final ColorPreferenceCompat colorPref = (ColorPreferenceCompat) preference;

                    int curColor = colorPref.getValue();
                    int r = (0xff0000 & curColor) >> 16;
                    int g = (0x00ff00 & curColor) >> 8;
                    int b = 0x0000ff & curColor;

                    ColorPicker picker = new ColorPicker(getActivity(), r, g, b);
                    picker.enableAutoClose();
                    picker.setCallback(colorPref::setValue);

                    picker.show();

                    return true;

                default:
                    return super.onPreferenceTreeClick(preference);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("lang")) {
                ViewUtil.showToast(getContext(), getString(R.string.closeToApply));
            }
        }
    }
}

