package com.qsp.player.util;

public final class StringUtil {

    public static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String getStringOrEmpty(String str) {
        return str != null ? str : "";
    }
}
