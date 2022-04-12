package com.qsp.player.shared.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PathUtil {

    @NonNull
    public static String getFilename(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(idx + 1);
    }

    @Nullable
    public static String getExtension(String path) {
        int idx = path.lastIndexOf('.');
        return idx == -1 ? null : path.substring(idx + 1);
    }

    @NonNull
    public static String removeExtension(String path) {
        int idx = path.lastIndexOf('.');
        return idx != -1 ? path.substring(0, idx) : path;
    }

    @NonNull
    public static String removeTrailingSlash(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(0, idx);
    }
}
