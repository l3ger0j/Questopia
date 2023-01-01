package org.qp.android.utils;

import android.content.Context;

import java.util.Locale;

public final class LanguageUtil {

    public static void setLocale(Context context, String lang) {
        var locale = new Locale(lang);
        var res = context.getResources();
        var dm = res.getDisplayMetrics();
        var conf = res.getConfiguration();
        conf.locale = locale;
        res.updateConfiguration(conf, dm);
    }
}
