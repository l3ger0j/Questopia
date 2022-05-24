package com.qsp.player.view.adapters;

import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;

public class SettingsAdapter {
    public boolean soundEnabled;
    public String language;
    public float actionsHeightRatio;
    public boolean isSeparator;
    public int typeface;
    public int fontSize;
    public boolean useGameFont;
    public int backColor;
    public int textColor;
    public int linkColor;

    @NonNull
    public static SettingsAdapter from(@NonNull SharedPreferences preferences) {
        SettingsAdapter settingsAdapter = new SettingsAdapter();
        settingsAdapter.soundEnabled = preferences.getBoolean("sound", true);
        settingsAdapter.language = preferences.getString("lang", "ru");
        settingsAdapter.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settingsAdapter.isSeparator = preferences.getBoolean("separator", false);
        settingsAdapter.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settingsAdapter.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settingsAdapter.useGameFont = preferences.getBoolean("useGameFont", false);
        settingsAdapter.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settingsAdapter.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settingsAdapter.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
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
