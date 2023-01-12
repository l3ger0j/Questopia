package org.qp.android.utils;

import android.app.Activity;
import android.content.Context;

import java.util.Locale;

@Deprecated
public final class LanguageUtil {
    private static String currentLanguage = Locale.getDefault().getLanguage();

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setLocaleOnContext(Context context, String lang) {
        if (!currentLanguage.equals(lang)) {
            var locale = new Locale(lang);
            var res = context.getResources();
            var dm = res.getDisplayMetrics();
            var conf = res.getConfiguration();
            conf.locale = locale;
            currentLanguage = locale.getLanguage();
            res.updateConfiguration(conf , dm);
        }
    }

    public static void setLocaleOnActivity(Activity activity, String languageCode) {
        if (!currentLanguage.equals(languageCode)) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            var resources = activity.getResources();
            var config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config , resources.getDisplayMetrics());
            currentLanguage = locale.getLanguage();
        }
    }
}
