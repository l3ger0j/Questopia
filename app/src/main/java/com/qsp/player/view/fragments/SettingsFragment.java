package com.qsp.player.view.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.qsp.player.BuildConfig;
import com.qsp.player.R;
import com.qsp.player.utils.ViewUtil;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private int countClick = 3;

    @NonNull
    public static SettingsFragment newInstance(String desc) {
        Bundle args = new Bundle();
        args.putString("desc", desc);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);
        String desc = requireArguments().getString("desc");

        Preference version = findPreference("showVersion");
        if (version != null) {
            version.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
            version.setOnPreferenceClickListener(preference -> {
                countClick--;
                if (countClick == 0) {
                    countClick = 3;
                    Toast.makeText(getContext(), "Eternity smells of oil", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
        
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