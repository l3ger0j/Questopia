package com.qsp.player.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.qsp.player.R;

import org.jetbrains.annotations.Contract;

public final class ViewUtil {

    public static void showSnackBar(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show();
    }

    public static void showErrorDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .show();
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
