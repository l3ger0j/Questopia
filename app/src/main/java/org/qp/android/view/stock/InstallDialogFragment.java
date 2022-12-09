package org.qp.android.view.stock;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;

public class InstallDialogFragment extends DialogFragment {
    private DialogInstallBinding installBinding;
    private DialogEditBinding editBinding;

    public void setInstallBinding(DialogInstallBinding installBinding) {
        this.installBinding = installBinding;
    }

    public void setEditBinding(DialogEditBinding editBinding) {
        this.editBinding = editBinding;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setView(installBinding != null ? installBinding.getRoot(): editBinding.getRoot())
                .create();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((StockActivity) requireActivity()).onDestroyDialogFragment();
        installBinding = null;
        editBinding = null;
        dismiss();
    }
}
