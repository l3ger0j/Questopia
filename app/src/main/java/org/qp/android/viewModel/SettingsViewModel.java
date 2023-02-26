package org.qp.android.viewModel;

import static org.qp.android.utils.ColorUtil.getHexColor;
import static org.qp.android.utils.ViewUtil.getFontStyle;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

import org.qp.android.R;
import org.qp.android.view.plugin.PluginFragment;
import org.qp.android.view.settings.SettingsActivity;
import org.qp.android.view.settings.SettingsController;

public class SettingsViewModel extends AndroidViewModel {
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
    public ObservableField<PluginFragment> fragmentObservableField =
            new ObservableField<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance().loadSettings(getApplication());
    }

    public String formationAboutDesc(@NonNull Context context) {
        return ABOUT_TEMPLATE
                .replace("QSPFONTSTYLE", getFontStyle(getSettingsController().getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(getSettingsController().fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(getSettingsController().textColor))
                .replace("QSPBACKCOLOR", getHexColor(getSettingsController().backColor))
                .replace("QSPLINKCOLOR", getHexColor(getSettingsController().linkColor))
                .replace("REPLACETEXT", context.getString(R.string.appDescription)
                        + context.getString(R.string.appCredits));
    }
}