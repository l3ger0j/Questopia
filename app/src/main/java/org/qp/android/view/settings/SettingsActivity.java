package org.qp.android.view.settings;

import static org.qp.android.utils.LanguageUtil.setLocale;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.R;
import org.qp.android.viewModel.viewModels.ActivitySettings;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity implements
        SettingsPatternPrefFrag.SettingsPatternFragmentList {
    private String currentLanguage = Locale.getDefault().getLanguage();
    private SettingsController settingsController;
    private ActivitySettings activitySettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        activitySettings = new ViewModelProvider(this)
                .get(ActivitySettings.class);
        activitySettings.settingsActivityObservableField.set(this);
        settingsController = SettingsController.newInstance().loadSettings(this);

        Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container,
                        SettingsFragment
                                .newInstance(activitySettings
                                        .formationAboutDesc(settingsController , this)),
                        "settingsFragment")
                .addToBackStack(null)
                .commit();

        loadLocale();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        var inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.bug_report) {
            var supportLink = Uri.parse("https://t.me/QuestPlayerHelper_bot");
            var intent = new Intent(Intent.ACTION_VIEW, supportLink);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return false;
    }

    @Override
    public void onClickShowPlugin(boolean onShow) {
        activitySettings.isShowPluginFragment = onShow;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (activitySettings.isShowPluginFragment) {
            onBackPressed();
        } else {
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        if (fragments.size() == 0) {
            super.onBackPressed();
        }
    }

    private void loadLocale() {
        setLocale(this, settingsController.language);
        currentLanguage = settingsController.language;
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsController = SettingsController.newInstance().loadSettings(this);
        updateLocale();
    }

    private void updateLocale() {
        if (currentLanguage.equals(settingsController.language)) return;
        setLocale(this, settingsController.language);
        currentLanguage = settingsController.language;
    }
}

