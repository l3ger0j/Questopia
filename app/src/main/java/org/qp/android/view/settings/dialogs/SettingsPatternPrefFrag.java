package org.qp.android.view.settings.dialogs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

public abstract class SettingsPatternPrefFrag extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public interface SettingsPatternFragmentList {
        void onClickShowPlugin (boolean onShow);
    }

    public SettingsPatternPrefFrag.SettingsPatternFragmentList listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SettingsPatternPrefFrag.SettingsPatternFragmentList) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PatternDialogListener");
        }
    }
}
