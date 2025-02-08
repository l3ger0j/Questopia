package org.qp.android.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.qp.android.R;
import org.qp.android.databinding.DialogImageBinding;
import org.qp.android.ui.game.GameViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameDialogFrags extends DialogFragment {

    public Uri pathToImage = Uri.EMPTY;
    private List<String> items;
    private GameDialogType dialogType;
    private DialogImageBinding imageBinding;
    private String processedMsg;
    private String message;
    private GameViewModel gameViewModel;
    private TextInputLayout feedBackName;
    private EditText feedBackNameET;
    private TextInputLayout feedBackContact;
    private EditText feedBackContactET;
    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (feedBackNameET != null) {
                validateUserName();
            } else if (feedBackContactET != null) {
                validateEmail();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public void setDialogType(GameDialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setProcessedMsg(String processedMsg) {
        this.processedMsg = processedMsg;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private boolean isValidate() {
        return validateUserName() && validateEmail();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
    }

    private boolean validateUserName() {
        if (feedBackNameET.getText().toString().trim().isEmpty()) {
            feedBackName.setError("Required Field!");
            feedBackNameET.requestFocus();
            return false;
        } else {
            feedBackName.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateEmail() {
        if (feedBackContactET.getText().toString().trim().isEmpty()) {
            feedBackContact.setError("Required Field!");
            feedBackContactET.requestFocus();
            return false;
        } else {
            feedBackContact.setErrorEnabled(false);
        }
        return true;
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
                pathToImage = savedInstanceState.getParcelable("pathToImage");
            }
            if (savedInstanceState.containsKey("processedMsg")) {
                processedMsg = savedInstanceState.getString("processedMsg");
            }
        }
        switch (dialogType) {
            case EXECUTOR_DIALOG, INPUT_DIALOG -> {
                final var executorView =
                        getLayoutInflater().inflate(R.layout.dialog_input, null);
                final var textInputLayout =
                        (TextInputLayout) executorView.findViewById(R.id.inputBox_edit);
                textInputLayout.setHelperText(message);
                builder.setView(executorView);
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> gameViewModel.onDialogPositiveClick(this));
                return builder.create();
            }
            case ERROR_DIALOG -> {
                final var errorFeBackView = getLayoutInflater().inflate(R.layout.dialog_feedback, null);
                var feedBackScrollError = (ScrollView) errorFeBackView.findViewById(R.id.feedBackScrollError);
                var feedBackTV = (TextView) feedBackScrollError.findViewById(R.id.feedBackTV);
                var optFeedBackName = Optional.ofNullable((TextInputLayout) errorFeBackView.findViewById(R.id.feedBackName));
                optFeedBackName.ifPresent(textInputLayout -> {
                    feedBackName = optFeedBackName.get();
                    var optEditText = Optional.ofNullable(optFeedBackName.get().getEditText());
                    optEditText.ifPresent(editText -> {
                        feedBackNameET = editText;
                        editText.addTextChangedListener(watcher);
                    });
                });
                var optFeedBackContact = Optional.ofNullable((TextInputLayout) errorFeBackView.findViewById(R.id.feedBackContact));
                optFeedBackContact.ifPresent(textInputLayout -> {
                    feedBackContact = optFeedBackContact.get();
                    var optEditText = Optional.ofNullable(optFeedBackContact.get().getEditText());
                    optEditText.ifPresent(editText -> {
                        feedBackContactET = editText;
                        editText.addTextChangedListener(watcher);
                    });
                });
                feedBackTV.setText(message);
                builder.setTitle(R.string.error);
                builder.setView(errorFeBackView);
                builder.setPositiveButton("Send", null);
                builder.setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                        });
                return builder.create();
            }
            case CLOSE_DIALOG -> {
                builder.setMessage(requireContext().getString(R.string.promptCloseGame));
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> gameViewModel.onDialogPositiveClick(this));
                builder.setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                        });
                return builder.create();
            }
            case IMAGE_DIALOG -> {
                imageBinding = DialogImageBinding.inflate(getLayoutInflater());
                imageBinding.imageBox.setImageURI(pathToImage);
                imageBinding.imageBox.setOnClickListener(v -> dismiss());
                builder.setView(imageBinding.getRoot());
                return builder.create();
            }
            case LOAD_DIALOG -> {
                builder.setMessage(requireContext().getString(R.string.loadGamePopup));
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> gameViewModel.onDialogPositiveClick(this));
                builder.setNegativeButton(android.R.string.no,
                        (dialog, which) -> {
                        });
                return builder.create();
            }
            case MENU_DIALOG -> {
                builder.setItems(items.toArray(new CharSequence[0]),
                        (dialog, which) -> gameViewModel.onDialogListClick(this, which));
                return builder.create();
            }
            case MESSAGE_DIALOG -> {
                builder.setMessage(processedMsg);
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> gameViewModel.onDialogPositiveClick(this));
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

        final var dialog = Optional.ofNullable((AlertDialog) getDialog());

        if (dialog.isPresent()) {
            if (dialogType.equals(GameDialogType.ERROR_DIALOG)) {
                var sendButton = dialog.get().getButton(Dialog.BUTTON_POSITIVE);
                sendButton.setOnClickListener(v -> {
                    if (isValidate()) {
                        gameViewModel.onDialogPositiveClick(this);
                        dialog.get().dismiss();
                    }
                });
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedDialogType", dialogType.toString());
        if (items != null) {
            outState.putStringArrayList("items", new ArrayList<>(items));
        }
        if (message != null) {
            outState.putString("message", message);
        }
        if (pathToImage != null) {
            outState.putParcelable("pathToImage", pathToImage);
        }
        if (processedMsg != null) {
            outState.putString("processedMsg", processedMsg);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (imageBinding != null) imageBinding = null;
    }
}
