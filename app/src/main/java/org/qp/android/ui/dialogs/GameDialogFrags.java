package org.qp.android.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.R;
import org.qp.android.databinding.DialogImageBinding;
import org.qp.android.ui.game.GameViewModel;

import java.util.ArrayList;
import java.util.Optional;

public class GameDialogFrags extends DialogFragment {
    private ArrayList<String> items;
    private GameDialogType dialogType;
    private DialogImageBinding imageBinding;
    private String processedMsg;
    private String message;
    private String template;

    private GameViewModel gameViewModel;

    public ObservableField<String> pathToImage = new ObservableField<>();

    public void setDialogType(GameDialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setItems(ArrayList<String> items) {
        this.items = items;
    }

    public void setProcessedMsg(String processedMsg) {
        this.processedMsg = processedMsg;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var builder = new MaterialAlertDialogBuilder(requireContext());
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("selectedDialogType")) {
                dialogType = GameDialogType.valueOf(savedInstanceState.getString("selectedDialogType"));
            }
            if (savedInstanceState.containsKey("items")) {
                items = savedInstanceState.getStringArrayList("items");
            }
            if (savedInstanceState.containsKey("message")) {
                message = savedInstanceState.getString("message");
            }
            if (savedInstanceState.containsKey("pathToImage")) {
                pathToImage.set(savedInstanceState.getString("pathToImage"));
            }
            if (savedInstanceState.containsKey("processedMsg")) {
                processedMsg = savedInstanceState.getString("processedMsg");
            }
        }
        switch (dialogType) {
            case EXECUTOR_DIALOG , INPUT_DIALOG -> {
                final var executorView =
                        getLayoutInflater().inflate(R.layout.dialog_input , null);
                var textInputLayout =
                        (TextInputLayout) executorView.findViewById(R.id.inputBox_edit);
                textInputLayout.setHelperText(message);
                builder.setView(executorView);
                builder.setPositiveButton(android.R.string.ok ,
                        (dialog , which) -> gameViewModel.onDialogPositiveClick(this));
                builder.setNeutralButton(null , (dialog , which) -> {});
                builder.setNeutralButtonIcon(ContextCompat.getDrawable(requireContext() ,
                        R.drawable.baseline_file_upload_24));
                return builder.create();
            }
            case ERROR_DIALOG -> {
                builder.setTitle(R.string.error);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok ,
                        (dialog , which) -> {});
                return builder.create();
            }
            case CLOSE_DIALOG -> {
                builder.setMessage(requireContext().getString(R.string.promptCloseGame));
                builder.setPositiveButton(android.R.string.ok ,
                        (dialog , which) -> gameViewModel.onDialogPositiveClick(this));
                builder.setNegativeButton(android.R.string.cancel ,
                        (dialog , which) -> {});
                return builder.create();
            }
            case IMAGE_DIALOG -> {
                imageBinding = DialogImageBinding.inflate(getLayoutInflater());
                imageBinding.setDialogFragment(this);
                imageBinding.imageBox.setOnClickListener(v -> dismiss());
                builder.setView(imageBinding.getRoot());
                return builder.create();
            }
            case LOAD_DIALOG -> {
                builder.setMessage(requireContext().getString(R.string.loadGamePopup));
                builder.setPositiveButton(android.R.string.ok ,
                        (dialog , which) -> gameViewModel.onDialogPositiveClick(this));
                builder.setNegativeButton(android.R.string.no ,
                        (dialog , which) -> {});
                return builder.create();
            }
            case MENU_DIALOG -> {
                builder.setItems(items.toArray(new CharSequence[0]) ,
                        (dialog , which) -> gameViewModel.onDialogListClick(this , which));
                return builder.create();
            }
            case MESSAGE_DIALOG -> {
                builder.setMessage(processedMsg);
                builder.setPositiveButton(android.R.string.ok ,
                        (dialog , which) -> gameViewModel.onDialogPositiveClick(this));
                return builder.create();
            }
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        gameViewModel.onDialogNegativeClick(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        var arguments = Optional.ofNullable(getArguments());
        if (arguments.isPresent()) {
            template = arguments.get().getString("template");
            final var dialog = Optional.ofNullable((AlertDialog) getDialog());
            if (dialog.isPresent()) {
                if (dialogType.equals(GameDialogType.EXECUTOR_DIALOG) ||
                        dialogType.equals(GameDialogType.INPUT_DIALOG)) {
                    var textInputLayout = (TextInputLayout) dialog.get().findViewById(R.id.inputBox_edit);
                    if (textInputLayout != null && textInputLayout.getEditText() != null) {
                        textInputLayout.getEditText().setText(template);
                    }
                    var neutralButton = (Button) dialog.get().getButton(Dialog.BUTTON_NEUTRAL);
                    neutralButton.setOnClickListener(v -> gameViewModel.onDialogNeutralClick(this));
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedDialogType", dialogType.toString());
        if (items != null) {
            outState.putStringArrayList("items", items);
        }
        if (message != null) {
            outState.putString("message" , message);
        }
        if (pathToImage != null) {
            outState.putString("pathToImage" , pathToImage.get());
        }
        if (processedMsg != null) {
            outState.putString("processedMsg", processedMsg);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageBinding != null)
            imageBinding = null;
    }
}
