package org.qp.android.view.stock;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;
import org.qp.android.utils.PatternDialogFragment;

public class StockDialogFragments extends PatternDialogFragment {
    private DialogInstallBinding installBinding;
    private DialogEditBinding editBinding;
    private StockDialogType dialogType;

    public void setDialogType(StockDialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setInstallBinding(DialogInstallBinding installBinding) {
        this.installBinding = installBinding;
    }

    public void setEditBinding(DialogEditBinding editBinding) {
        this.editBinding = editBinding;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (installBinding == null && editBinding == null) {
            dismissAllowingStateLoss();
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        switch (dialogType) {
            case INSTALL_DIALOG:
                builder.setView(installBinding.getRoot());
                return builder.create();
            case EDIT_DIALOG:
                builder.setView(editBinding.getRoot());
                return builder.create();
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedDialogType", dialogType.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener.onDialogPositiveClick(this);
        if (installBinding != null) {
            installBinding = null;
        }
        if (editBinding != null) {
            editBinding = null;
        }
    }
}
