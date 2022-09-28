package com.qsp.player.view.activities;

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

public class SettingsActivity extends AppCompatActivity {
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
        SettingsAdapter settingsAdapter = settingsActivityVM.loadSettings(this);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment(
                        settingsActivityVM.formationAboutDesc(settingsAdapter , this)))
                .commit();
    }
}

