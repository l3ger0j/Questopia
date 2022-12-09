package org.qp.android.view.game;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.qp.android.R;

public class InputDialogFragment extends DialogFragment {
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    EditText editView = view.findViewById(R.id.inputBox_edit);
                    ((GameActivity) requireActivity()).onClickOk(editView.getText().toString());
                })
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("message", message);
    }
}
