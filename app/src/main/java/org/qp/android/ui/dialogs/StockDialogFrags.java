package org.qp.android.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.qp.android.R;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.ui.stock.StockViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class StockDialogFrags extends DialogFragment {

    private DialogAddBinding addBinding;
    private DialogEditBinding editBinding;
    private StockDialogType dialogType;
    private ArrayList<String> names;

    private boolean isInstalled;
    private String message;
    private String title;

    private StockViewModel stockViewModel;

    public void setDialogType(StockDialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setAddBinding(DialogAddBinding addBinding) {
        this.addBinding = addBinding;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var builder = new MaterialAlertDialogBuilder(requireContext());
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
        switch (dialogType) {
            case ADD_DIALOG -> {
                if (addBinding != null) {
                    builder.setView(addBinding.getRoot());
                    return builder.create();
                } else {
                    dismissAllowingStateLoss();
                    return super.onCreateDialog(savedInstanceState);
                }
            }
            case DELETE_DIALOG -> {
                if (Objects.equals(message, "1")) {
                    builder.setTitle("Delete a folder?");
                } else {
                    builder.setTitle("Delete a folders?");
                }
                builder.setPositiveButton(android.R.string.ok , (dialog , which) ->
                        stockViewModel.outputIntObserver.setValue(1));
                builder.setNegativeButton(android.R.string.cancel , (dialog , which) ->
                        stockViewModel.outputIntObserver.setValue(0));
                return builder.create();
            }
            case EDIT_DIALOG -> {
                if (editBinding != null) {
                    builder.setView(editBinding.getRoot());
                    return builder.create();
                } else {
                    dismissAllowingStateLoss();
                    return super.onCreateDialog(savedInstanceState);
                }
            }
            case ERROR_DIALOG -> {
                builder.setTitle(R.string.error);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok , (dialog , which) -> {
                });
                return builder.create();
            }
            case MIGRATION_DIALOG -> {
                builder.setTitle("Migration is available");
                builder.setMessage("Previous entries have been found. Do you want to migrate them to the database?");
                builder.setPositiveButton(android.R.string.ok, (dialog , which) ->
                        stockViewModel.startMigration());
                builder.setNegativeButton(android.R.string.cancel, (dialog , which) -> {});
                return builder.create();
            }
            case SELECT_DIALOG -> {
                builder.setTitle(requireContext().getString(R.string.selectGameFile));
                builder.setItems(names.toArray(new String[0]) , (dialog , which) ->
                        stockViewModel.outputIntObserver.setValue(which));
                return builder.create();
            }
            default -> {
                dismissAllowingStateLoss();
                return super.onCreateDialog(savedInstanceState);
            }
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
        if (addBinding != null) {
            addBinding = null;
        }
        if (editBinding != null) {
            editBinding = null;
        }
    }
}
