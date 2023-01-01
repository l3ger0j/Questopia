package org.qp.android.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PathUtil {

    @NonNull
    public static String normalizeFolderName(@NonNull String name) {
        var result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;
        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    @NonNull
    public static String getFilename(@NonNull String path) {
        var idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(idx + 1);
    }

    @Nullable
    public static String getExtension(@NonNull String path) {
        var idx = path.lastIndexOf('.');
        return idx == -1 ? null : path.substring(idx + 1);
    }

    @NonNull
    public static String removeExtension(@NonNull String path) {
        var idx = path.lastIndexOf('.');
        return idx != -1 ? path.substring(0, idx) : path;
    }

    @NonNull
    public static String removeTrailingSlash(@NonNull String path) {
        var idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(0, idx);
    }
}
