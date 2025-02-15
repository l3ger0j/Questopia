package org.qp.android.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.R;

public class SettingTextFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState,
                                    @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.setting_text, rootKey);

        var textColorPref = findPreference("textColor");
        if (textColorPref != null) {
            textColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#000000"));
        }
        var backColorPref = findPreference("backColor");
        if (backColorPref != null) {
            backColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#e0e0e0"));
        }
        var linkColorPref = findPreference("linkColor");
        if (linkColorPref != null) {
            linkColorPref.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#0000ff"));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        var rootIns = ViewCompat.getRootWindowInsets(view);
        if (rootIns == null) return;
        var ins = rootIns.getInsets(
                WindowInsetsCompat.Type.systemBars() |
                        WindowInsetsCompat.Type.displayCutout()
        );

        view.setPadding(
                view.getPaddingStart() + ins.left,
                view.getPaddingTop(),
                view.getPaddingRight() + ins.right,
                view.getPaddingBottom() + ins.bottom
        );
    }
}
