package com.qsp.player.view.activities;

import static com.qsp.player.utils.ColorUtil.getHexColor;
import static com.qsp.player.utils.LanguageUtil.setLocale;
import static com.qsp.player.utils.ViewUtil.getFontStyle;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.qsp.player.R;
import com.qsp.player.utils.ViewUtil;
import com.qsp.player.view.adapters.SettingsAdapter;
import com.qsp.player.viewModel.viewModels.SettingsActivityViewModel;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private static final String ABOUT_TEMPLATE = "<html><head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1\">\n" +
            "<style type=\"text/css\">\n" +
            "body{margin: 0; padding: 0; color: QSPTEXTCOLOR; background-color: QSPBACKCOLOR; max-width: 100%; font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "a{color: QSPLINKCOLOR; }\n" +
            "a:link{color: QSPLINKCOLOR; }\n" +
            "table{font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "</style></head><body>REPLACETEXT</body></html>";

    private SettingsAdapter settingsAdapter;
    private String mDesc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsActivityViewModel settingsActivityViewModel = new ViewModelProvider(this)
                .get(SettingsActivityViewModel.class);
        settingsActivityViewModel.settingsActivityObservableField.set(this);

        loadSettings();
        loadLocale();
        formationAboutDesc();

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment(mDesc, settingsAdapter.url))
                .commit();
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsAdapter = SettingsAdapter.from(preferences);
    }

    private void loadLocale() {
        setLocale(this, settingsAdapter.language);
    }

    private void formationAboutDesc() {
        mDesc = ABOUT_TEMPLATE
                .replace("QSPFONTSTYLE", getFontStyle(settingsAdapter.typeface))
                .replace("QSPFONTSIZE", Integer.toString(settingsAdapter.fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(settingsAdapter.textColor))
                .replace("QSPBACKCOLOR", getHexColor(settingsAdapter.backColor))
                .replace("QSPLINKCOLOR", getHexColor(settingsAdapter.linkColor))
                .replace("REPLACETEXT", getString(R.string.appDescription) + getString(R.string.appCredits));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final String desc;
        private final String url;

        SettingsFragment (String desc, String url) {
            this.desc = desc;
            this.url = url;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            requireActivity().setTitle(R.string.settings);
            addPreferencesFromResource(R.xml.settings);

            Preference button = findPreference("showAbout");
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    View messageView = getLayoutInflater()
                            .inflate(R.layout.dialog_about, null, false);

                    WebView descView = messageView.findViewById(R.id.about_descrip);
                    descView.loadDataWithBaseURL("", desc,
                            "text/html", "utf-8", "");

                    new AlertDialog.Builder(getContext())
                            .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                            })
                            .setView(messageView)
                            .create()
                            .show();
                    return true;
                });
            }

            StringBuilder urlSummary = new StringBuilder();
            if (!url.isEmpty()) {
                urlSummary.append(getString(R.string.summaryURL).replace("-URL-", url));
            }
            EditTextPreference urlPref = findPreference("url");
            if (urlPref != null) {
                urlPref.setSummary(urlSummary);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("lang") || key.equals("url")) {
                ViewUtil.showToast(getContext(), getString(R.string.closeToApply));
            }
        }
    }
}

