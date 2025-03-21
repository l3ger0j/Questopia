package org.qp.android.ui.dialogs;

import static org.qp.android.helpers.utils.PathUtil.removeExtension;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_IMAGE_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_MOD_FILE;
import static org.qp.android.ui.stock.StockViewModel.CODE_PICK_PATH_FILE;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.anggrayudi.storage.file.MimeType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.ui.stock.GameDataObserver;
import org.qp.android.ui.stock.StockViewModel;

import java.security.SecureRandom;
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
    private DocumentFile newDirEntry;
    private String[] folderLocations;

    private final GameDataObserver dataObserver = new GameDataObserver();
    private StockViewModel stockViewModel;

    public void setDialogType(StockDialogType dialogType) {
        this.dialogType = dialogType;
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

    public void setNewDirEntry(DocumentFile newDirEntryName) {
        this.newDirEntry = newDirEntryName;
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
        var builder = new MaterialAlertDialogBuilder(
                requireContext(),
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
        );

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
                addBinding = DialogAddBinding.inflate(getLayoutInflater());
                addBinding.setGame(dataObserver);

                var titleText = addBinding.ET0.getEditText();
                if (titleText != null) {
                    titleText.setText(newDirEntry.getName());
                }

                addBinding.buttonSelectIcon.setOnClickListener(v ->
                        stockViewModel.doOnShowFilePicker(CODE_PICK_IMAGE_FILE, new String[]{MimeType.IMAGE}));
                addBinding.addBT.setOnClickListener(v -> {
                    var nameDir = newDirEntry.getName();
                    if (nameDir == null) {
                        var secureRandom = new SecureRandom();
                        nameDir = "game#" + secureRandom.nextInt();
                    }

                    var unfilledEntry = new Game();
                    var editTextTitle = addBinding.ET0.getEditText();
                    if (editTextTitle != null) {
                        var entryTitle = editTextTitle.getText().toString();
                        unfilledEntry.title = !isNotEmpty(entryTitle)
                                ? nameDir
                                : editTextTitle.getText().toString();
                    }
                    var editTextAuthor = addBinding.ET1.getEditText();
                    if (editTextAuthor != null) {
                        unfilledEntry.author = editTextAuthor.getText().toString();
                    }
                    var editTextVersion = addBinding.ET2.getEditText();
                    if (editTextVersion != null) {
                        unfilledEntry.version = editTextVersion.getText().toString();
                    }

                    stockViewModel.createAddIntent(unfilledEntry, newDirEntry);
                });

                builder.setView(addBinding.getRoot());
                return builder.create();
            }
            case EDIT_DIALOG -> {
                editBinding = DialogEditBinding.inflate(getLayoutInflater());

                dataObserver.isModDirExist.set(stockViewModel.isModsDirExist());
                dataObserver.iconUriObserver.set(stockViewModel.currGameEntry.gameIconUri);
                editBinding.setGame(dataObserver);

                editBinding.buttonSelectPath.setOnClickListener(v ->
                        stockViewModel.doOnShowFilePicker(CODE_PICK_PATH_FILE, new String[]{MimeType.BINARY_FILE}));
                editBinding.buttonSelectMod.setOnClickListener(v ->
                        stockViewModel.doOnShowFilePicker(CODE_PICK_MOD_FILE, new String[]{MimeType.BINARY_FILE}));
                editBinding.buttonSelectIcon.setOnClickListener(v ->
                        stockViewModel.doOnShowFilePicker(CODE_PICK_IMAGE_FILE, new String[]{MimeType.IMAGE}));
                editBinding.editBT.setOnClickListener(v -> {
                    var unfilledEntry = new Game();
                    var editTextTitle = editBinding.ET0.getEditText();
                    if (editTextTitle != null) {
                        unfilledEntry.title = editTextTitle.getText().toString().isEmpty()
                                ? removeExtension(unfilledEntry.title)
                                : editTextTitle.getText().toString();
                    }

                    var editTextAuthor = editBinding.ET1.getEditText();
                    if (editTextAuthor != null) {
                        unfilledEntry.author = editTextAuthor.getText().toString().isEmpty()
                                ? removeExtension(unfilledEntry.author)
                                : editTextAuthor.getText().toString();
                    }

                    var editTextVersion = editBinding.ET2.getEditText();
                    if (editTextVersion != null) {
                        unfilledEntry.version = editTextVersion.toString().isEmpty()
                                ? removeExtension(unfilledEntry.version)
                                : editTextVersion.getText().toString();
                    }

                    stockViewModel.createEditIntent(unfilledEntry);
                });

                builder.setView(editBinding.getRoot());
                return builder.create();
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
                // TODO MIGRATION!
                builder.setPositiveButton(android.R.string.ok, (dialog , which) -> {});
                builder.setNegativeButton(android.R.string.cancel, (dialog , which) -> {});
                return builder.create();
            }
            case GAME_FOLDER_INIT -> {
                builder.setIcon(R.drawable.folder_add_24);
                builder.setTitle("Folder location");
                builder.setSingleChoiceItems(folderLocations, 1, (dialog, which) -> {
                });
                builder.setPositiveButton(android.R.string.ok, (dialog , which) -> {});
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
    public void onResume() {
        super.onResume();

        stockViewModel.fileMutableLiveData = new MutableLiveData<>();
        stockViewModel.fileMutableLiveData.observe(this, file -> {
            switch (file.fileType()) {
                case IMAGE_FILE -> {
                    if (editBinding == null) {
                        addBinding.buttonSelectIcon.setText(file.inputFile().getName());
                        dataObserver.iconUriObserver.set(file.inputFile().getUri());
                    } else {
                        editBinding.buttonSelectIcon.setText(file.inputFile().getName());
                        dataObserver.iconUriObserver.set(file.inputFile().getUri());
                    }
                }
                case PATH_FILE ->
                        editBinding.buttonSelectPath.setText(file.inputFile().getName());
                case MOD_FILE ->
                        editBinding.buttonSelectMod.setText(file.inputFile().getName());
            }
        });
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
