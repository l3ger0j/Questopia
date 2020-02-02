package com.qsp.player.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.qsp.player.R;

import java.util.Locale;

public final class ViewUtil {

    public static void setLocale(Context context, String lang) {
        Locale locale;

        if (lang.equals("zh-rTW")) {  // TAIWAN
            locale = Locale.TAIWAN;
        } else if (lang.equals("zh-rCN")) {  // CHINA
            locale = Locale.CHINA;
        } else if (!lang.contains("-r")) {  // lang doesn't contain a region code
            locale = new Locale(lang);
        } else {  // lang is not TAIWAN, CHINA, or short, use country+region
            String region = lang.substring(lang.indexOf("-r") + 2);
            locale = new Locale(lang, region);
        }

        Resources newRes = context.getResources();
        DisplayMetrics dm = newRes.getDisplayMetrics();

        Configuration conf = newRes.getConfiguration();
        conf.locale = locale;

        newRes.updateConfiguration(conf, dm);
    }

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showErrorDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public static String getFontStyle(String typeface) {
        switch (typeface) {
            case "2":
                return "serif";
            case "3":
                return "courier";
            case "0":
            case "1":
            default:
                return "sans-serif";
        }
    }
}
