package org.qp.android.ui.settings;

import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_GDIR_FILE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceFragmentCompat;

import org.qp.android.BuildConfig;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.helpers.utils.ViewUtil;
import org.qp.android.ui.dialogs.SettingsDialogFrag;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener  {
    private int countClick = 3;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        requireActivity().setTitle(R.string.settingsTitle);
        addPreferencesFromResource(R.xml.settings);

        var viewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        var customWidthImage = findPreference("customWidth");
        if (customWidthImage != null) {
            customWidthImage.setEnabled(!viewModel.getSettingsController().isUseAutoWidth);
        }

        var customHeightImage = findPreference("customHeight");
        if (customHeightImage != null) {
            customHeightImage.setEnabled(!viewModel.getSettingsController().isUseAutoHeight);
        }

        var textColor = findPreference("textColor");
        if (textColor != null) {
            textColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#000000"));
            textColor.setEnabled(!viewModel.getSettingsController().isUseGameTextColor);
        }

        var backColor = findPreference("backColor");
        if (backColor != null) {
            backColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#e0e0e0"));
            backColor.setEnabled(!viewModel.getSettingsController().isUseGameBackgroundColor);
        }

        var linkColor = findPreference("linkColor");
        if (linkColor != null) {
            linkColor.setSummary(getString(R.string.textBackLinkColorSum)
                    .replace("-VALUE-", "#0000ff"));
            linkColor.setEnabled(!viewModel.getSettingsController().isUseGameLinkColor);
        }

        var click = findPreference("showExtensionMenu");
        if (click != null) {
            click.setOnPreferenceClickListener(preference -> {
                Navigation.findNavController(requireView())
                        .navigate(R.id.pluginFragment);
                return true;
            });
        }
        var news = findPreference("newsApp");
        if (news != null) {
            news.setOnPreferenceClickListener(preference -> {
                Navigation.findNavController(requireView())
                        .navigate(R.id.newsFragment);
                return false;
            });
        }

        var rootDir = findPreference("addRootFold");
        if (rootDir != null) {
            var application = (QuestPlayerApplication) requireActivity().getApplication();
            if (application.getCustomRootDir() == null) {
                rootDir.setSummary(getString(R.string.addFolderSum0));
            } else {
                rootDir.setSummary(getString(R.string.addFolderSum1));
            }
            rootDir.setOnPreferenceClickListener(preference -> {
                var activity = (SettingsActivity) requireActivity();
                activity.showDirPickerDialog(CODE_PICK_GDIR_FILE);
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
                var webView = new WebView(requireContext());
                webView.loadDataWithBaseURL(
                        "",
                        viewModel.formationAboutDesc(requireContext()),
                        "text/html",
                        "utf-8",
                        "");
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
                        Toast.makeText(requireContext(), libQspProxy.getCompiledDateTime()
                                +"\n"+libQspProxy.getVersionQSP(), Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException ex) {
                        return true;
                    }
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Objects.equals(key , "lang")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.closeToApply));
        } else if (key.equals("binPref")) {
            ViewUtil.showSnackBar(getView(), getString(R.string.settingsEffect));
        }
    }
}