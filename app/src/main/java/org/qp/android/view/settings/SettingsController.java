package org.qp.android.view.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class SettingsController {
    public int typeface;
    public int fontSize;
    public int backColor;
    public int textColor;
    public int linkColor;
    public float actionsHeightRatio;
    public boolean isSoundEnabled;
    public boolean useOldValue;
    public boolean useSeparator;
    public boolean useGameFont;
    public boolean useAutoscroll;
    public boolean useExecString;
    public String language;

    private static SettingsController INSTANCE;

    public static SettingsController newInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingsController();
        }
        return INSTANCE;
    }

    public SettingsController loadSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return from(preferences);
    }

    @NonNull
    private static SettingsController from(@NonNull SharedPreferences preferences) {
        SettingsController settingsController = new SettingsController();
        settingsController.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settingsController.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settingsController.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settingsController.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settingsController.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
        settingsController.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settingsController.useOldValue = preferences.getBoolean("oldValue", true);
        settingsController.useAutoscroll = preferences.getBoolean("autoscroll", true);
        settingsController.useExecString = preferences.getBoolean("execString", false);
        settingsController.useSeparator = preferences.getBoolean("separator", false);
        settingsController.useGameFont = preferences.getBoolean("gameFont", false);
        settingsController.isSoundEnabled = preferences.getBoolean("sound", true);
        settingsController.language = preferences.getString("lang", "ru");
        return settingsController;
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
