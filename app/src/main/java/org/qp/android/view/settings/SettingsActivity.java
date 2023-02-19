package org.qp.android.view.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.R;
import org.qp.android.view.settings.dialogs.SettingsPatternPrefFrag;
import org.qp.android.viewModel.SettingsViewModel;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements
        SettingsPatternPrefFrag.SettingsPatternFragmentList {
    private SettingsController settingsController;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsViewModel = new ViewModelProvider(this)
                .get(SettingsViewModel.class);
        settingsViewModel.settingsActivityObservableField.set(this);
        settingsController = SettingsController.newInstance().loadSettings(this);

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container,
                        SettingsFragment
                                .newInstance(settingsViewModel
                                        .formationAboutDesc(settingsController , this)),
                        "settingsFragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsController = SettingsController.newInstance().loadSettings(this);
    }

    @Override
    public void onClickShowPlugin(boolean onShow) {
        settingsViewModel.isShowPluginFragment = onShow;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (settingsViewModel.isShowPluginFragment) {
            onBackPressed();
        } else {
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        var manager = getSupportFragmentManager();
        var fragments = manager.getFragments();
        if (fragments.size() == 0) {
            super.onBackPressed();
        }
    }
}

