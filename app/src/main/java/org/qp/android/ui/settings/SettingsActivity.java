package org.qp.android.ui.settings;

import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.qp.android.R;
import org.qp.android.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding settingsViewBinding;
    private final NavController.OnDestinationChangedListener listener =
            (navController, navDestination, bundle) -> {
                if (navDestination.getLabel() != null) {
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
                }
            };
    private NavController navController;

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

        ViewCompat.setOnApplyWindowInsetsListener(settingsViewBinding.getRoot(), (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            var mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        setContentView(settingsViewBinding.getRoot());

        if (getSupportActionBar() != null) {
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
        switch (settingsViewModel.getSettingsController().theme) {
            case "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            case "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
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
        navController.removeOnDestinationChangedListener(listener);
    }
}
