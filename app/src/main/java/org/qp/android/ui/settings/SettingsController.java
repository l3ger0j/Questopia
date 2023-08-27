package org.qp.android.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class SettingsController {
    public int typeface;
    public int fontSize;
    public int backColor;
    public int textColor;
    public int linkColor;
    public int customWidthImage;
    public int customHeightImage;
    public int binaryPrefixes;
    public float actionsHeightRatio;
    public boolean isSoundEnabled;
    public boolean isUseAutoWidth;
    public boolean isUseAutoHeight;
    public boolean isUseSeparator;
    public boolean isUseGameFont;
    public boolean isUseNewFilePicker;
    public boolean isUseAutoscroll;
    public boolean isUseExecString;
    public boolean isUseImmersiveMode;
    public boolean isUseGameTextColor;
    public boolean isUseGameBackgroundColor;
    public boolean isUseGameLinkColor;
    public boolean isUseFullscreenImages;
    public boolean isUseImageDebug;
    public boolean isUseMusicDebug;
    public String language;

    private static SettingsController INSTANCE;

    @NonNull
    public static SettingsController newInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SettingsController();
        }
        return from(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public Typeface getTypeface() {
        return switch (typeface) {
            case 1 -> Typeface.SANS_SERIF;
            case 2 -> Typeface.SERIF;
            case 3 -> Typeface.MONOSPACE;
            default -> Typeface.DEFAULT;
        };
    }

    @NonNull
    private static SettingsController from(@NonNull SharedPreferences preferences) {
        SettingsController settingsController = new SettingsController();
        settingsController.typeface = Integer.parseInt(preferences.getString("typeface", "0"));
        settingsController.fontSize = Integer.parseInt(preferences.getString("fontSize", "16"));
        settingsController.binaryPrefixes = Integer.parseInt(preferences.getString("binPref","1000"));
        settingsController.actionsHeightRatio = parseActionsHeightRatio(preferences.getString("actsHeight", "1/3"));
        settingsController.isUseNewFilePicker = preferences.getBoolean("filePicker", false);
        settingsController.isUseAutoscroll = preferences.getBoolean("autoscroll", true);
        settingsController.isUseExecString = preferences.getBoolean("execString", false);
        settingsController.isUseImageDebug = preferences.getBoolean("debugImage", false);
        settingsController.isUseMusicDebug = preferences.getBoolean("debugMusic", false);
        settingsController.isUseSeparator = preferences.getBoolean("separator", false);
        settingsController.isUseGameFont = preferences.getBoolean("useGameFont", false);
        settingsController.isUseImmersiveMode = preferences.getBoolean("immersiveMode", true);
        settingsController.isSoundEnabled = preferences.getBoolean("sound", true);
        settingsController.language = preferences.getString("lang", "ru");
        imageSettings(settingsController, preferences);
        colorSettings(settingsController , preferences);
        return settingsController;
    }

    private static void colorSettings (@NonNull SettingsController settingsController,
                                       @NonNull SharedPreferences preferences) {
        settingsController.isUseGameTextColor = preferences.getBoolean("useGameTextColor", true);
        settingsController.textColor = preferences.getInt("textColor", Color.parseColor("#000000"));
        settingsController.isUseGameBackgroundColor = preferences.getBoolean("useGameBackgroundColor", true);
        settingsController.backColor = preferences.getInt("backColor", Color.parseColor("#e0e0e0"));
        settingsController.isUseGameLinkColor = preferences.getBoolean("useGameLinkColor", true);
        settingsController.linkColor = preferences.getInt("linkColor", Color.parseColor("#0000ff"));
    }

    private static void imageSettings (@NonNull SettingsController settingsController,
                                       @NonNull SharedPreferences preferences) {
        settingsController.isUseAutoWidth = preferences.getBoolean("autoWidth", true);
        settingsController.customWidthImage = Integer.parseInt(preferences.getString("customWidthImage", "400"));
        settingsController.isUseAutoHeight = preferences.getBoolean("autoHeight", true);
        settingsController.customHeightImage = Integer.parseInt(preferences.getString("customHeightImage", "400"));
        settingsController.isUseFullscreenImages = preferences.getBoolean("fullScreenImage", false);
    }

    private static float parseActionsHeightRatio(@NonNull String str) {
        return switch (str) {
            case "1/5" -> 0.2f;
            case "1/4" -> 0.25f;
            case "1/3" -> 0.33f;
            case "1/2" -> 0.5f;
            case "2/3" -> 0.67f;
            default -> throw new RuntimeException("Unsupported value of actsHeight: " + str);
        };
    }
}
