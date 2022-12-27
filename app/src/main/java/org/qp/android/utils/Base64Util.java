package org.qp.android.utils;

import static android.util.Base64.DEFAULT;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Base64Util {
    private static final String BASE64_PATTERN = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
    private static final Pattern pattern = Pattern.compile(BASE64_PATTERN);

    public static String encodeBase64(String str) {
        return encodeBase64(str, DEFAULT);
    }

    public static String encodeBase64(String str, int flags) {
        return Base64.encodeToString(str.getBytes(), flags);
    }

    @NonNull
    @Contract("_ -> new")
    public static String decodeBase64(String base64) {
        return new String(Base64.decode(base64, DEFAULT));
    }

    public static boolean hasBase64 (String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }
}
