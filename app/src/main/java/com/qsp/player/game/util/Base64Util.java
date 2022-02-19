package com.qsp.player.game.util;

import android.util.Base64;

import static android.util.Base64.DEFAULT;

public final class Base64Util {

    public static String encodeBase64(String str) {
        return encodeBase64(str, DEFAULT);
    }

    public static String encodeBase64(String str, int flags) {
        return Base64.encodeToString(str.getBytes(), flags);
    }

    public static String decodeBase64(String base64) {
        return new String(Base64.decode(base64, DEFAULT));
    }
}
