package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.LanguageUtil.setLocale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.qsp.player.model.libQSP.QspListItem;
import com.qsp.player.view.adapters.SettingsAdapter;

import java.util.ArrayList;

public class GameActivityVM extends ViewModel {

    public ObservableField<String> mainDesc = new ObservableField<>();
    public ObservableField<ArrayList<QspListItem>> action = new ObservableField<>();

    public SettingsAdapter loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return SettingsAdapter.from(preferences);
    }

    public String loadLocale(Context context, @NonNull SettingsAdapter settingsAdapter) {
        setLocale(context, settingsAdapter.language);
        return settingsAdapter.language;
    }

}
