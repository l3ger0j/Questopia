package org.qp.android.view.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.model.libQSP.LibQspProxy;
import org.qp.android.utils.ViewUtil;
import org.qp.android.view.plugin.PluginFragment;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private int countClick = 3;

    @NonNull
    public static SettingsFragment newInstance(String desc) {
        Bundle args = new Bundle();
        args.putString("desc", desc);
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);
        String desc = requireArguments().getString("desc");

        Preference backColor = findPreference("backColor");
        Objects.requireNonNull(backColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#e0e0e0"));

        Preference textColor = findPreference("textColor");
        Objects.requireNonNull(textColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#000000"));

        Preference linkColor = findPreference("linkColor");
        Objects.requireNonNull(linkColor).setSummary(getString(R.string.textBackLinkColorSum)
                .replace("-VALUE-", "#0000ff"));

        Preference click = findPreference("showExtensionMenu");
        if (click != null) {
            click.setOnPreferenceClickListener(preference -> {
                PluginFragment pluginFragment = new PluginFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_container, pluginFragment , "pluginFragment")
                        .addToBackStack(null)
                        .commit();
                return true;
            });
        }
        
        Preference button = findPreference("showAbout");
        if (button != null) {
            button.setOnPreferenceClickListener(preference -> {
                LinearLayout linearLayout = new LinearLayout(getContext());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams linLayoutParam =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                linearLayout.setLayoutParams(linLayoutParam);

                LinearLayout.LayoutParams lpView =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                WebView webView = new WebView(getContext());
                webView.loadDataWithBaseURL("", desc,
                        "text/html", "utf-8", "");
                webView.setLayoutParams(lpView);
                linearLayout.addView(webView);

                new AlertDialog.Builder(getContext())
                        .setView(linearLayout)
                        .create()
                        .show();
                return true;
            });
        }

        Preference version = findPreference("showVersion");
        if (version != null) {
            version.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
            version.setOnPreferenceClickListener(preference -> {
                countClick--;
                if (countClick == 0) {
                    countClick = 3;
                    QuestPlayerApplication application = (QuestPlayerApplication) requireActivity().getApplication();
                    LibQspProxy libQspProxy = application.getLibQspProxy();
                    try {
                        Toast.makeText(getContext(), libQspProxy.getCompiledDateTime()
                                +"\n"+libQspProxy.getVersionQSP(), Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException ex) {
                        Toast.makeText(getContext(),
                                "Follow the white rabbit", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });
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
        if (key.equals("lang")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.closeToApply));
        }
    }
}