package com.qsp.player.view.fragments;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;
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

        Preference backColor = findPreference("backColor");
        Objects.requireNonNull(backColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#e0e0e0"));

        Preference textColor = findPreference("textColor");
        Objects.requireNonNull(textColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#000000"));

        Preference linkColor = findPreference("linkColor");
        Objects.requireNonNull(linkColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#0000ff"));

        Preference version = findPreference("showVersion");
        if (version != null) {
            version.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
            version.setOnPreferenceClickListener(preference -> {
                countClick--;
                if (countClick == 0) {
                    countClick = 3;
                    Toast.makeText(getContext(), "I challenge you to all out life", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
        
        Preference button = findPreference("showAbout");
        if (button != null) {
            button.setOnPreferenceClickListener(preference -> {
                LinearLayout linearLayout = new LinearLayout(getContext());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams linLayoutParam =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                linearLayout.setLayoutParams(linLayoutParam);

                LinearLayout.LayoutParams lpView =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                WebView webView = new WebView(getContext());
                webView.loadDataWithBaseURL("", desc,
                        "text/html", "utf-8", "");
                webView.setLayoutParams(lpView);
                linearLayout.addView(webView);

                new AlertDialog.Builder(getContext())
                        .setView(linearLayout)
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
        if (key.equals("lang")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.closeToApply));
        }
    }
}