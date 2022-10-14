package com.qsp.player.view.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class SettingsAdapter {
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

    private static SettingsAdapter INSTANCE;

    public static SettingsAdapter newInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingsAdapter();
        }
        return INSTANCE;
    }

    public SettingsAdapter loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return from(preferences);
    }

    @NonNull
    private static SettingsAdapter from(@NonNull SharedPreferences preferences) {
        SettingsAdapter settingsAdapter = new SettingsAdapter();
        settingsAdapter.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settingsAdapter.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settingsAdapter.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settingsAdapter.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settingsAdapter.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
        settingsAdapter.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settingsAdapter.useRotate = preferences.getBoolean("rotate", false);
        settingsAdapter.useOldValue = preferences.getBoolean("oldValue", true);
        settingsAdapter.useAutoscroll = preferences.getBoolean("autoscroll", true);
        settingsAdapter.useSeparator = preferences.getBoolean("separator", false);
        settingsAdapter.useGameFont = preferences.getBoolean("gameFont", false);
        settingsAdapter.isSoundEnabled = preferences.getBoolean("sound", true);
        settingsAdapter.language = preferences.getString("lang", "ru");
        return settingsAdapter;
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
