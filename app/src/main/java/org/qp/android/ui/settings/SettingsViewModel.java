package org.qp.android.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

public class SettingsViewModel extends AndroidViewModel {

    private static final String ABOUT_RU_LINK = "https://questopia.site/about-ru.html";
    private static final String ABOUT_EN_LINK = "https://questopia.site/about-en.html";
    public ObservableField<SettingsActivity> settingsActivityObservableField = new ObservableField<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    public String getLinkAboutDesc() {
        if (getSettingsController().language.equalsIgnoreCase("ru")) {
            return ABOUT_RU_LINK;
        } else if (getSettingsController().language.equalsIgnoreCase("en")) {
            return ABOUT_EN_LINK;
        }
        return ABOUT_EN_LINK;
    }
}