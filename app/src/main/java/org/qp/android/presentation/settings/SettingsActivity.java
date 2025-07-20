package org.qp.android.presentation.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import org.qp.android.R;
import org.qp.android.databinding.ActivitySettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding settingsViewBinding;
    private final NavController.OnDestinationChangedListener listener =
            (navController, navDestination, bundle) -> {
                if (navDestination.getLabel() == null) return;
                switch (String.valueOf(navDestination.getLabel())) {
                    case "SettingsFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.settingsTitle));
                    case "SettingGeneralFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.generalCatTitle));
                    case "SettingTextFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.textCatTitle));
                    case "SettingImageFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.imageCatTitle));
                    case "SettingSoundFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.soundCatTitle));
                    case "PluginFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.pluginTitle));
                    case "NewsFragment" ->
                            settingsViewBinding.settingsColToolbar.setTitle(this.getText(R.string.newsMenuTitle));
                }
            };
    private NavController navController;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, key) -> {
        if (key == null) return;
        switch (key) {
            case "lang" -> {
                switch (sharedPreferences.getString("lang", "en")) {
                    case "ru" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"));
                    case "en" -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
                }
            }
            case "theme" -> {
                switch (sharedPreferences.getString("theme", "auto")) {
                    case "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    case "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    case "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Prevent jumping of the player on devices with cutout
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        }

        settingsViewBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setSupportActionBar(settingsViewBinding.settingsToolbar);

        setContentView(settingsViewBinding.getRoot());

        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        var navFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.settingsFragHost);
        if (navFragment != null) {
            navController = navFragment.getNavController();
        }

        navController.addOnDestinationChangedListener(listener);

        if (savedInstanceState == null) {
            navController.navigate(R.id.settingsFragment);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() != null) {
                    var currCharLabel = navController.getCurrentDestination().getLabel();
                    if (currCharLabel == null) return;
                    switch (currCharLabel.toString()) {
                        case "SettingGeneralFragment", "SettingTextFragment",
                             "SettingImageFragment", "SettingSoundFragment" -> {
                            setTitle(R.string.settingsTitle);
                            navController.navigate(R.id.settingsFragment);
                        }
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
                case "PluginFragment", "NewsFragment" ->
                        getOnBackPressedDispatcher().onBackPressed();
                case "SettingGeneralFragment", "SettingTextFragment",
                     "SettingImageFragment", "SettingSoundFragment" -> {
                    setTitle(R.string.settingsTitle);
                    navController.navigate(R.id.settingsFragment);
                }
                default -> finish();
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager
                .getDefaultSharedPreferences(getApplication())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        navController.removeOnDestinationChangedListener(listener);
    }
}
