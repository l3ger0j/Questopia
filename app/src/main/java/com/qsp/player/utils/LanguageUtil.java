package com.qsp.player.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

public final class LanguageUtil {

    public static void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        Configuration conf = res.getConfiguration();
        conf.locale = locale;

        res.updateConfiguration(conf, dm);
    }
}
