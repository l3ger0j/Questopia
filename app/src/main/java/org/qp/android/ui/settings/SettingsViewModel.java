package org.qp.android.ui.settings;

import static org.qp.android.helpers.utils.ColorUtil.getHexColor;
import static org.qp.android.helpers.utils.FileUtil.readAssetFileAsString;
import static org.qp.android.helpers.utils.ViewUtil.getFontStyle;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

public class SettingsViewModel extends AndroidViewModel {

    public ObservableField<SettingsActivity> settingsActivityObservableField = new ObservableField<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    public String formationAboutDesc(@NonNull Context context) {
        var aboutPage = "";
        if (getSettingsController().language.equalsIgnoreCase("ru")) {
            aboutPage = readAssetFileAsString(context , "about-ru.html");
        } else if (getSettingsController().language.equalsIgnoreCase("en")) {
            aboutPage = readAssetFileAsString(context , "about-en.html");
        }
        if (aboutPage == null || aboutPage.isEmpty()) return null;
        return aboutPage
                .replace("QSPFONTSTYLE", getFontStyle(getSettingsController().getTypeface()))
                .replace("QSPFONTSIZE", Integer.toString(getSettingsController().fontSize))
                .replace("QSPTEXTCOLOR", getHexColor(getSettingsController().textColor))
                .replace("QSPBACKCOLOR", getHexColor(getSettingsController().backColor))
                .replace("QSPLINKCOLOR", getHexColor(getSettingsController().linkColor));
    }
}