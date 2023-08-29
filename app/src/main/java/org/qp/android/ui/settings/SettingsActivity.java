package org.qp.android.ui.settings;

import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_GDIR_FILE;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.anggrayudi.storage.SimpleStorageHelper;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private NavController navController;
    private final SimpleStorageHelper storageHelper = new SimpleStorageHelper(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        storageHelper.setOnFolderSelected((integer , documentFile) -> {
            if (documentFile != null) {
                if (integer == CODE_PICK_GDIR_FILE) {
                    var application = (QuestPlayerApplication) getApplication();
                    if (application != null) application.setCustomRootDir(documentFile);
                }
            }

            return null;
        });

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public void showDirPickerDialog(int requestCode) {
        storageHelper.openFolderPicker(requestCode);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController.getCurrentDestination() != null) {
            if (Objects.equals(navController
                    .getCurrentDestination()
                    .getLabel() , "PluginFragment")) {
                onBackPressed();
            } else if (Objects.equals(navController
                    .getCurrentDestination()
                    .getLabel() , "NewsFragment")) {
                onBackPressed();
            } else {
                finish();
            }
        }
        return true;
    }
}

