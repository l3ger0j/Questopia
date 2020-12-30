package com.qsp.player.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.qsp.player.R;

import java.util.Locale;

public final class ViewUtil {

    public static void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        Configuration conf = res.getConfiguration();
        conf.locale = locale;

        res.updateConfiguration(conf, dm);
    }

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showErrorDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
    }

    public static String getFontStyle(int typeface) {
        switch (typeface) {
            case 2:
                return "serif";
            case 3:
                return "courier";
            case 0:
            case 1:
            default:
                return "sans-serif";
        }
    }
}
