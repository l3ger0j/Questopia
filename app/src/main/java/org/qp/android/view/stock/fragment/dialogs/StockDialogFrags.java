package org.qp.android.view.stock.fragment.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;

import java.util.ArrayList;

public class StockDialogFrags extends StockPatternDialogFrags {
    private DialogInstallBinding installBinding;
    private DialogEditBinding editBinding;
    private StockDialogType dialogType;
    private ArrayList<String> names;

    private boolean isInstalled;
    private String message;
    private String title;

    public void setDialogType(StockDialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setInstallBinding(DialogInstallBinding installBinding) {
        this.installBinding = installBinding;
    }

    public void setEditBinding(DialogEditBinding editBinding) {
        this.editBinding = editBinding;
    }

    public void setNames(ArrayList<String> names) {
        this.names = names;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null ) {
            if (savedInstanceState.containsKey("selectedDialogType")) {
                dialogType = StockDialogType.valueOf(savedInstanceState.getString("selectedDialogType"));
            }
            if (savedInstanceState.containsKey("arrayListNames")) {
                names = savedInstanceState.getStringArrayList("arrayListNames");
            }
            if (savedInstanceState.containsKey("booleanIsInstalled")) {
                isInstalled = savedInstanceState.getBoolean("booleanIsInstalled");
            }
            if (savedInstanceState.containsKey("stringMessage")) {
                message = savedInstanceState.getString("stringMessage");
            }
            if (savedInstanceState.containsKey("stringTitle")) {
                title = savedInstanceState.getString("stringTitle");
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        switch (dialogType) {
            case EDIT_DIALOG:
                if (editBinding != null) {
                    builder.setView(editBinding.getRoot());
                    return builder.create();
                } else {
                    dismissAllowingStateLoss();
                    return super.onCreateDialog(savedInstanceState);
                }
            case ERROR_DIALOG:
                builder.setTitle(R.string.error);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {});
                return builder.create();
            case INSTALL_DIALOG:
                if (installBinding != null) {
                    builder.setView(installBinding.getRoot());
                    return builder.create();
                } else {
                    dismissAllowingStateLoss();
                    return super.onCreateDialog(savedInstanceState);
                }
            case INFO_DIALOG:
                builder.setMessage(message);
                builder.setTitle(title);
                builder.setIcon(R.mipmap.ic_launcher_round);
                builder.setNegativeButton(getString(R.string.close), (dialog, which) -> dialog.cancel());
                if (isInstalled) {
                    builder.setNeutralButton(getString(R.string.play), (dialog, which) ->
                            listener.onDialogNeutralClick(this));
                    builder.setPositiveButton(getString(R.string.editButton), (dialog, which) ->
                            listener.onDialogPositiveClick(this));
                }
                return builder.create();
            case SELECT_DIALOG:
                builder.setTitle(requireContext().getString(R.string.selectGameFile));
                builder.setItems(names.toArray(new String[0]), (dialog, which) ->
                        listener.onDialogListClick(this, which));
                return builder.create();

            default:
                dismissAllowingStateLoss();
                return super.onCreateDialog(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedDialogType", dialogType.toString());
        outState.putBoolean("booleanIsInstalled", isInstalled);
        if (names != null) {
            outState.putStringArrayList("arrayListNames", names);
        }
        if (message != null) {
            outState.putString("stringMessage", message);
        }
        if (title != null) {
            outState.putString("stringTitle", title);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener.onDialogDestroy(this);
        if (installBinding != null) {
            installBinding = null;
        }
        if (editBinding != null) {
            editBinding = null;
        }
    }
}
