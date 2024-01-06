package org.qp.android.helpers.utils;

import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Contract;

public final class ViewUtil {

    public static void showSnackBar(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    @NonNull
    @Contract(pure = true)
    public static String getFontStyle(Typeface typeface) {
        if (Typeface.SANS_SERIF.equals(typeface)) {
            return "sans-serif";
        } else if (Typeface.SERIF.equals(typeface)) {
            return "serif";
        } else if (Typeface.MONOSPACE.equals(typeface)) {
            return "courier";
        } else if (Typeface.DEFAULT.equals(typeface)) {
            return "default";
        }
        return "default";
    }
}
