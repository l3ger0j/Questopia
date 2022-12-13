package org.qp.android.view.game;


import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.qp.android.utils.PatternDialogFragment;

import java.util.ArrayList;

public class ShowMenuDialogFragment extends PatternDialogFragment {
    private ArrayList<String> items;

    public void setItems(ArrayList<String> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) ->
                        listener.onDialogListClick(this, which))
                .setOnCancelListener(dialog ->
                        listener.onDialogNegativeClick(this))
                .create();
    }
}
