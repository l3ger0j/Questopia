package com.qsp.player.view.activities;

import static com.qsp.player.utils.LanguageUtil.setLocale;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.R;
import com.qsp.player.viewModel.viewModels.ActivitySettings;

import java.util.Locale;

public class Settings extends AppCompatActivity {
    private String currentLanguage = Locale.getDefault().getLanguage();
    private com.qsp.player.view.adapters.Settings settings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActivitySettings activitySettings = new ViewModelProvider(this)
                .get(ActivitySettings.class);
        activitySettings.settingsActivityObservableField.set(this);
        settings = com.qsp.player.view.adapters.Settings.newInstance().loadSettings(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, com.qsp.player.view.fragments.Settings
                        .newInstance(activitySettings.formationAboutDesc(settings , this)))
                .commit();

        loadLocale();
    }

    private void loadLocale() {
        setLocale(this, settings.language);
        currentLanguage = settings.language;
    }

    @Override
    protected void onResume() {
        super.onResume();
        settings = com.qsp.player.view.adapters.Settings.newInstance().loadSettings(this);
        updateLocale();
    }

    private void updateLocale() {
        if (currentLanguage.equals(settings.language)) return;
        setLocale(this, settings.language);
        currentLanguage = settings.language;
    }
}

