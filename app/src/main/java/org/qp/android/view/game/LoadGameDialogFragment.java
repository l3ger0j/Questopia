package org.qp.android.view.game;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.qp.android.R;
import org.qp.android.utils.PatternDialogFragment;

public class LoadGameDialogFragment extends PatternDialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setMessage(requireContext().getString(R.string.loadGamePopup))
                .setPositiveButton(android.R.string.yes, (dialog, which) ->
                        listener.onDialogPositiveClick(this))
                .setNegativeButton(android.R.string.no, (dialog, which) -> { })
                .create();
    }
}
