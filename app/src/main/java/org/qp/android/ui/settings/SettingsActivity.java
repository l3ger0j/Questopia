package org.qp.android.ui.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.qp.android.R;

public class SettingsActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null ) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        var navFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.settingsFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }

        var settingsViewModel = new ViewModelProvider(this)
                .get(SettingsViewModel.class);
        settingsViewModel.settingsActivityObservableField.set(this);

        if (settingsViewModel.getSettingsController().language.equals("ru")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
        }

        if (savedInstanceState == null) {
            navController.navigate(R.id.settingsFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController.getCurrentDestination() != null) {
            var currCharLabel = navController.getCurrentDestination().getLabel();
            if (currCharLabel == null) return true;
            Log.d(this.getClass().getSimpleName() , currCharLabel.toString());
            switch (currCharLabel.toString()) {
                case "PluginFragment" , "NewsFragment" ->
                        getOnBackPressedDispatcher().onBackPressed();
                case "SettingGeneralFragment" , "SettingTextFragment" , "SettingImageFragment" ->
                        navController.navigate(R.id.settingsFragment);
                default -> finish();
            }
        }
        return true;
    }
}

