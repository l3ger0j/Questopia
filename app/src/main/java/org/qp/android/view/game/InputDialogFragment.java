package org.qp.android.view.game;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.qp.android.R;
import org.qp.android.utils.PatternDialogFragment;

public class InputDialogFragment extends PatternDialogFragment {
    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final View view =
                getLayoutInflater().inflate(R.layout.dialog_input, null);
        if (savedInstanceState != null && savedInstanceState.containsKey("message")) {
            message = savedInstanceState.getString("message");
        }
        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        listener.onDialogPositiveClick(this))
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("message", message);
    }
}
