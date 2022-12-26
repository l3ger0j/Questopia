package org.qp.android.utils;

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
    public static String getFontStyle(int typeface) {
        switch (typeface) {
            case 2:
                return "serif";
            case 3:
                return "courier";
            case 0:
            case 1:
            default:
                return "sans-serif";
        }
    }
}
