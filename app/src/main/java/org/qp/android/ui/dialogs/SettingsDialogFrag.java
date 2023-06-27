package org.qp.android.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsDialogFrag extends DialogFragment {
    private View view;

    public void setView(View view) {
        this.view = view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (view != null) {
            return new MaterialAlertDialogBuilder(requireContext())
                    .setView(view)
                    .create();
        } else {
            dismissAllowingStateLoss();
            return super.onCreateDialog(savedInstanceState);
        }
    }
}
