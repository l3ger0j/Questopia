package org.qp.android.view.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.utils.ViewUtil;
import org.qp.android.view.plugin.PluginFragment;
import org.qp.android.view.settings.dialogs.SettingsDialogFrag;
import org.qp.android.view.settings.dialogs.SettingsPatternPrefFrag;

import java.util.Objects;

public class SettingsFragment extends SettingsPatternPrefFrag {
    private int countClick = 3;

    @NonNull
    public static SettingsFragment newInstance(String desc) {
        var args = new Bundle();
        args.putString("desc", desc);
        var fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);
        var desc = requireArguments().getString("desc");

        var controller = SettingsController
                .newInstance().loadSettings(getContext());

        var customWidthImage = findPreference("customWidth");
        if (customWidthImage != null) {
            customWidthImage.setEnabled(!controller.isUseAutoWidth);
        }

        var customHeightImage = findPreference("customHeight");
        if (customHeightImage != null) {
            customHeightImage.setEnabled(!controller.isUseAutoHeight);
        }

        var textColor = findPreference("textColor");
        if (textColor != null) {
            textColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#000000"));
            textColor.setEnabled(!controller.isUseGameTextColor);
        }

        var backColor = findPreference("backColor");
        if (backColor != null) {
            backColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#e0e0e0"));
            backColor.setEnabled(!controller.isUseGameBackgroundColor);
        }

        var linkColor = findPreference("linkColor");
        if (linkColor != null) {
            linkColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#0000ff"));
            linkColor.setEnabled(!controller.isUseGameLinkColor);
        }

        var click = findPreference("showExtensionMenu");
        if (click != null) {
            click.setOnPreferenceClickListener(preference -> {
                listener.onClickShowPlugin(true);
                var pluginFragment = new PluginFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_container, pluginFragment , "pluginFragment")
                        .addToBackStack(null)
                        .commit();
                return true;
            });
        }
        
        var button = findPreference("showAbout");
        if (button != null) {
            button.setOnPreferenceClickListener(preference -> {
                var linearLayout = new LinearLayout(getContext());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                var linLayoutParam =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                linearLayout.setLayoutParams(linLayoutParam);
                var lpView =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                var webView = new WebView(getContext());
                webView.loadDataWithBaseURL("", desc,
                        "text/html", "utf-8", "");
                webView.setLayoutParams(lpView);
                linearLayout.addView(webView);
                var dialogFrag = new SettingsDialogFrag();
                dialogFrag.setView(linearLayout);
                dialogFrag.show(getParentFragmentManager(), "settingsDialogFragment");
                return true;
            });
        }

        var version = findPreference("showVersion");
        if (version != null) {
            version.setTitle(getString(R.string.extendedName)
                    .replace("-VERSION-", BuildConfig.VERSION_NAME));
            version.setOnPreferenceClickListener(preference -> {
                countClick--;
                if (countClick == 0) {
                    countClick = 3;
                    var application = (QuestPlayerApplication) requireActivity().getApplication();
                    var libQspProxy = application.getLibQspProxy();
                    try {
                        Toast.makeText(getContext(), libQspProxy.getCompiledDateTime()
                                +"\n"+libQspProxy.getVersionQSP(), Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException ex) {
                        Toast.makeText(getContext(),
                                "▒▒▒▒▒▒▒▒▄▄▄▄▄▄▄▄▒▒▒▒▒▒▒▒\n" +
                                "▒▒▒▒▒▄█▀▀░░░░░░▀▀█▄▒▒▒▒▒\n" +
                                "▒▒▒▄█▀▄██▄░░░░░░░░▀█▄▒▒▒\n" +
                                "▒▒█▀░▀░░▄▀░░░░▄▀▀▀▀░▀█▒▒\n" +
                                "▒█▀░░░░███░░░░▄█▄░░░░▀█▒\n" +
                                "▒█░░░░░░▀░░░░░▀█▀░░░░░█▒\n" +
                                "▒█░░░░░░░░░░░░░░░░░░░░█▒\n" +
                                "▒█░░██▄░░▀▀▀▀▄▄░░░░░░░█▒\n" +
                                "▒▀█░█░█░░░▄▄▄▄▄░░░░░░█▀▒\n" +
                                "▒▒▀█▀░▀▀▀▀░▄▄▄▀░░░░▄█▀▒▒\n" +
                                "▒▒▒█░░░░░░▀█░░░░░▄█▀▒▒▒▒\n" +
                                "▒▒▒█▄░░░░░▀█▄▄▄█▀▀▒▒▒▒▒▒\n" +
                                "▒▒▒▒▀▀▀▀▀▀▀▒▒▒▒▒▒▒▒▒▒▒▒▒\n", Toast.LENGTH_SHORT).show();
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
        } else if (key.equals("binPref")) {
            ViewUtil.showSnackBar(getView(), "The setting will take effect the next time you unpack the game");
        }
    }
}