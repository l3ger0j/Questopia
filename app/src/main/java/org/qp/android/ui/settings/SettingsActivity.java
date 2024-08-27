package org.qp.android.ui.settings;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.qp.android.R;
import org.qp.android.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var settingsViewBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setSupportActionBar(settingsViewBinding.settingsToolbar);
        setContentView(settingsViewBinding.getRoot());

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

        getOnBackPressedDispatcher().addCallback(this , new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() != null) {
                    var currCharLabel = navController.getCurrentDestination().getLabel();
                    if (currCharLabel == null) return;
                    switch (currCharLabel.toString()) {
                        case "SettingGeneralFragment" , "SettingTextFragment" ,
                                "SettingImageFragment", "SettingSoundFragment" ->
                                navController.navigate(R.id.settingsFragment);
                        default -> finish();
                    }
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController.getCurrentDestination() != null) {
            var currCharLabel = navController.getCurrentDestination().getLabel();
            if (currCharLabel == null) return true;
            switch (currCharLabel.toString()) {
                case "PluginFragment" , "NewsFragment" ->
                        getOnBackPressedDispatcher().onBackPressed();
                case "SettingGeneralFragment" , "SettingTextFragment" ,
                        "SettingImageFragment" , "SettingSoundFragment" ->
                        navController.navigate(R.id.settingsFragment);
                default -> finish();
            }
        }
        return true;
    }
}

