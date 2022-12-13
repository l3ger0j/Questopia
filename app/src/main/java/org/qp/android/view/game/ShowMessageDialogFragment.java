package org.qp.android.view.game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.qp.android.utils.PatternDialogFragment;

public class ShowMessageDialogFragment extends PatternDialogFragment {
    private String processedMsg;

    public void setProcessedMsg(String processedMsg) {
        this.processedMsg = processedMsg;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setMessage(processedMsg)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        listener.onDialogPositiveClick(this))
                .setCancelable(false)
                .create();
    }
}
