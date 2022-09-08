package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.ColorUtil.getHexColor;
import static com.qsp.player.utils.ViewUtil.getFontStyle;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;
import androidx.preference.PreferenceManager;

import com.qsp.player.R;
import com.qsp.player.view.activities.SettingsActivity;
import com.qsp.player.view.adapters.SettingsAdapter;

public class SettingsActivityVM extends ViewModel {
    private static final String ABOUT_TEMPLATE = "<html><head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1\">\n" +
            "<style type=\"text/css\">\n" +
            "body{margin: 0; padding: 0; color: QSPTEXTCOLOR; background-color: QSPBACKCOLOR; max-width: 100%; font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "a{color: QSPLINKCOLOR; }\n" +
            "a:link{color: QSPLINKCOLOR; }\n" +
            "table{font-size: QSPFONTSIZE; font-family: QSPFONTSTYLE; }\n" +
            "</style></head><body>REPLACETEXT</body></html>";


    public ObservableField<SettingsActivity> settingsActivityObservableField =
            new ObservableField<>();

    public SettingsAdapter loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return SettingsAdapter.from(preferences);
    }

    public String formationAboutDesc(SettingsAdapter settingsAdapter, Context context) {
        return ABOUT_TEMPLATE
                .replace("QSPFONTSTYLE", getFontStyle(settingsAdapter.typeface))
                .replace("QSPFONTSIZE", Integer.toString(settingsAdapter.fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(settingsAdapter.textColor))
                .replace("QSPBACKCOLOR", getHexColor(settingsAdapter.backColor))
                .replace("QSPLINKCOLOR", getHexColor(settingsAdapter.linkColor))
                .replace("REPLACETEXT", context.getString(R.string.appDescription)
                        + context.getString(R.string.appCredits));
    }
}