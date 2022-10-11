package com.qsp.player.view.activities;

import static com.qsp.player.utils.LanguageUtil.setLocale;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.BuildConfig;
import com.qsp.player.R;
import com.qsp.player.view.adapters.SettingsAdapter;
import com.qsp.player.view.fragments.SettingsFragment;
import com.qsp.player.viewModel.viewModels.SettingsActivityVM;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private String currentLanguage = Locale.getDefault().getLanguage();
    private SettingsAdapter settingsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        TextView textView = findViewById(R.id.settingsTV);
        textView.setText(getString(R.string.extendedName)
                .replace("-VERSION-", BuildConfig.VERSION_NAME));

        SettingsActivityVM settingsActivityVM = new ViewModelProvider(this)
                .get(SettingsActivityVM.class);
        settingsActivityVM.settingsActivityObservableField.set(this);
        settingsAdapter = SettingsAdapter.newInstance().loadSettings(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment
                        .newInstance(settingsActivityVM.formationAboutDesc(settingsAdapter, this)))
                .commit();

        loadLocale();
    }

    private void loadLocale() {
        setLocale(this, settingsAdapter.language);
        currentLanguage = settingsAdapter.language;
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsAdapter = SettingsAdapter.newInstance().loadSettings(this);
        updateLocale();
    }

    private void updateLocale() {
        if (currentLanguage.equals(settingsAdapter.language)) return;
        setLocale(this, settingsAdapter.language);
        currentLanguage = settingsAdapter.language;
    }
}

