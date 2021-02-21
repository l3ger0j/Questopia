package com.qsp.player;

import android.content.SharedPreferences;
import android.graphics.Color;

public class Settings {
    private boolean soundEnabled;
    private String language;
    private float actionsHeightRatio;
    private int typeface;
    private int fontSize;
    private boolean useGameFont;
    private int backColor;
    private int textColor;
    private int linkColor;

    public static Settings from(SharedPreferences preferences) {
        Settings settings = new Settings();
        settings.soundEnabled = preferences.getBoolean("sound", true);
        settings.language = preferences.getString("lang", "ru");
        settings.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settings.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settings.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settings.useGameFont = preferences.getBoolean("useGameFont", false);
        settings.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settings.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settings.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
        return settings;
    }

    private static float parseActionsHeightRatio(String str) {
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

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getLanguage() {
        return language;
    }

    public float getActionsHeightRatio() {
        return actionsHeightRatio;
    }

    public int getTypeface() {
        return typeface;
    }

    public int getFontSize() {
        return fontSize;
    }

    public boolean isUseGameFont() {
        return useGameFont;
    }

    public int getBackColor() {
        return backColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getLinkColor() {
        return linkColor;
    }
}
