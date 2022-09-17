package com.qsp.player.view.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.qsp.player.R;
import com.qsp.player.utils.ViewUtil;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String desc;

    public SettingsFragment(String desc) {
        this.desc = desc;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);

        Preference button = findPreference("showAbout");
        if (button != null) {
            button.setOnPreferenceClickListener(preference -> {
                View messageView = getLayoutInflater()
                        .inflate(R.layout.dialog_about, null, false);

                WebView descView = messageView.findViewById(R.id.about_descrip);
                descView.loadDataWithBaseURL("", desc,
                        "text/html", "utf-8", "");

                new AlertDialog.Builder(getContext())
                        .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        })
                        .setView(messageView)
                        .create()
                        .show();
                return true;
            });
        }
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("lang") || key.equals("url")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.closeToApply));
        }
    }
}