package com.qsp.player.view.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class Settings {
    public int typeface;
    public int fontSize;
    public int backColor;
    public int textColor;
    public int linkColor;
    public float actionsHeightRatio;
    public boolean isSoundEnabled;
    public boolean useRotate;
    public boolean useOldValue;
    public boolean useSeparator;
    public boolean useGameFont;
    public boolean useAutoscroll;
    public String language;

    private static Settings INSTANCE;

    public static Settings newInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Settings();
        }
        return INSTANCE;
    }

    public Settings loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return from(preferences);
    }

    @NonNull
    private static Settings from(@NonNull SharedPreferences preferences) {
        Settings settings = new Settings();
        settings.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settings.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settings.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settings.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settings.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
        settings.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settings.useRotate = preferences.getBoolean("rotate", false);
        settings.useOldValue = preferences.getBoolean("oldValue", true);
        settings.useAutoscroll = preferences.getBoolean("autoscroll", true);
        settings.useSeparator = preferences.getBoolean("separator", false);
        settings.useGameFont = preferences.getBoolean("gameFont", false);
        settings.isSoundEnabled = preferences.getBoolean("sound", true);
        settings.language = preferences.getString("lang", "ru");
        return settings;
    }

    private static float parseActionsHeightRatio(@NonNull String str) {
        switch (str) {
            case "1/5":
                return 0.2f;
            case "1/4":
                return 0.25f;
            case "1/3":
                return 0.33f;
            case "1/2":
                return 0.5f;
            case "2/3":
                return 0.67f;
            default:
                throw new RuntimeException("Unsupported value of actsHeight: " + str);
        }
    }
}
