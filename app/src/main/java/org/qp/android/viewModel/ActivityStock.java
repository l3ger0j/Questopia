package org.qp.android.viewModel;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static org.qp.android.utils.FileUtil.GAME_INFO_FILENAME;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.createFile;
import static org.qp.android.utils.FileUtil.dirSize;
import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.findFileRecursively;
import static org.qp.android.utils.FileUtil.formatFileSize;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;
import static org.qp.android.utils.FileUtil.isWritableDirectory;
import static org.qp.android.utils.FileUtil.isWritableFile;
import static org.qp.android.utils.PathUtil.normalizeFolderName;
import static org.qp.android.utils.PathUtil.removeExtension;
import static org.qp.android.utils.XmlUtil.objectToXml;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.model.install.InstallException;
import org.qp.android.model.install.Installer;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.utils.ArchiveUtil;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.settings.SettingsController;
import org.qp.android.view.stock.StockActivity;
import org.qp.android.view.stock.fragment.dialogs.StockDialogFrags;
import org.qp.android.view.stock.fragment.dialogs.StockDialogType;
import org.qp.android.viewModel.repository.LocalGame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ActivityStock extends AndroidViewModel {
    private static final int PICKFILE_RESULT_CODE = 101;
    private final String TAG = this.getClass().getSimpleName();

    public ObservableField<StockActivity> activityObservableField = new
            ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();
    public ObservableBoolean isSelectArchive = new ObservableBoolean();
    public ObservableBoolean isSelectFolder = new ObservableBoolean();

    private final LocalGame localGame = new LocalGame();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private DocumentFile tempInstallFile, tempInstallDir, tempImageFile, tempPathFile, tempModFile;

    private DialogInstallBinding installBinding;
    private GameData tempGameData;
    private DialogEditBinding editBinding;
    private SettingsController controller;

    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();

    // region Getter/Setter
    public void setController(SettingsController controller) {
        this.controller = controller;
    }

    public GameData getTempGameData() {
        return tempGameData;
    }

    public void setTempGameData(GameData tempGameData) {
        this.tempGameData = tempGameData;
    }

    public void setTempPathFile(DocumentFile tempPathFile) {
        this.tempPathFile = tempPathFile;
        editBinding.fileTV.setText(tempPathFile.getName());
    }

    public void setTempModFile(DocumentFile tempModFile) {
        this.tempModFile = tempModFile;
        editBinding.modTV.setText(tempModFile.getName());
    }

    public void setTempInstallFile(@NonNull DocumentFile tempInstallFile) {
        this.tempInstallFile = tempInstallFile;
        installBinding.fileTV.setText(tempInstallFile.getName());
    }

    public void setTempInstallDir(@NonNull DocumentFile tempInstallDir) {
        this.tempInstallDir = tempInstallDir;
        installBinding.buttonSelectArchive.setEnabled(false);
        installBinding.folderTV.setText(tempInstallDir.getName());
    }

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
    }

    public void setGamesDir(File gamesDir) {
        this.gamesDir = gamesDir;
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }
    // endregion Getter/Setter

    public ActivityStock(@NonNull Application application) {
        super(application);
    }

    // region Dialog
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    public void showDialogInstall() {
        dialogFragments.setDialogType(StockDialogType.INSTALL_DIALOG);
        dialogFragments.setInstallBinding(formingInstallView());
        dialogFragments.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
        Objects.requireNonNull(activityObservableField.get())
                .showInstallDialogFragment(dialogFragments);
        isShowDialog.set(true);
    }

    public void createInstallIntent() {
        var gameData = new GameData();
        try {
            gameData.id = removeExtension(Objects.requireNonNull(tempInstallFile != null ?
                    tempInstallFile.getName()
                    : tempInstallDir.getName()));
            gameData.title = (Objects.requireNonNull(
                    installBinding.ET0.getEditText()).getText().toString().isEmpty() ?
                    removeExtension(Objects.requireNonNull(tempInstallFile != null ?
                            tempInstallFile.getName() :
                            tempInstallDir.getName()))
                    : Objects.requireNonNull(
                    installBinding.ET0.getEditText()).getText().toString());
            gameData.author = (Objects.requireNonNull(
                    installBinding.ET1.getEditText()).getText().toString().isEmpty() ?
                    null
                    : Objects.requireNonNull(
                    installBinding.ET1.getEditText()).getText().toString());
            gameData.version = (Objects.requireNonNull(
                    installBinding.ET2.getEditText()).getText().toString().isEmpty() ?
                    null
                    : Objects.requireNonNull(
                    installBinding.ET2.getEditText()).getText().toString());
            if (tempInstallFile != null) {
                gameData.fileSize = formatFileSize(tempInstallFile.length(), controller.binaryPrefixes);
            } else if (tempInstallDir != null) {
                gameData.fileSize = formatFileSize(dirSize(tempInstallDir), controller.binaryPrefixes);
            }
            gameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
            installGame(tempInstallFile != null ? tempInstallFile : tempInstallDir, gameData);
            isSelectArchive.set(false);
            isSelectFolder.set(false);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Error: " + ex);
        }
    }

    public void showDialogEdit() {
        dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
        dialogFragments.setEditBinding(formingEditView());
        dialogFragments.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
        Objects.requireNonNull(activityObservableField.get())
                .showEditDialogFragment(dialogFragments);
        isShowDialog.set(true);
    }

    public void playGame() {
        var intent = new Intent(activityObservableField.get(),
                GameActivity.class);
        intent.putExtra("gameId", tempGameData.id);
        intent.putExtra("gameTitle", tempGameData.title);
        intent.putExtra("gameDirUri", tempGameData.gameDir.getAbsolutePath());
        var gameFileCount = tempGameData.gameFiles.size();
        switch (gameFileCount) {
            case 0:
                Log.w(TAG, "GameData has no game files");
                break;
            case 1:
                intent.putExtra("gameFileUri", tempGameData.gameFiles.get(0).getAbsolutePath());
                Objects.requireNonNull(activityObservableField.get()).startActivity(intent);
                break;
            default:
                if (outputIntObserver.hasObservers()) {
                    outputIntObserver = new MutableLiveData<>();
                }
                var names = new ArrayList<String>();
                for (var file : tempGameData.gameFiles) {
                    names.add(file.getName());
                }
                var dialogFragments = new StockDialogFrags();
                dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
                dialogFragments.setNames(names);
                Objects.requireNonNull(activityObservableField.get())
                        .showSelectDialogFragment(dialogFragments);
                outputIntObserver.observeForever(integer -> {
                    intent.putExtra("gameFileUri",
                            tempGameData.gameFiles.get(integer).getAbsolutePath());
                    Objects.requireNonNull(activityObservableField.get()).startActivity(intent);
                });
                break;
        }
    }

    public void createEditIntent() {
        try {
            var editTextTitle = editBinding.ET0.getEditText();
            if (editTextTitle != null) {
                if (editTextTitle.getText().toString().isEmpty()) {
                    tempGameData.title = removeExtension(tempGameData.title);
                } else {
                    tempGameData.title = editTextTitle.getText().toString();
                }
            }
            var editTextAuthor = editBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                if (editTextAuthor.getText().toString().isEmpty()) {
                    tempGameData.author = removeExtension(tempGameData.author);
                } else {
                    tempGameData.author = editTextAuthor.getText().toString();
                }
            }
            var editTextVersion = editBinding.ET2.getEditText();
            if (editTextVersion != null) {
                if (editTextVersion.toString().isEmpty()) {
                    tempGameData.version = removeExtension(tempGameData.version);
                } else {
                    tempGameData.version = editTextVersion.getText().toString();
                }
            }
            if (tempImageFile != null) {
                tempGameData.icon = tempImageFile.getUri().toString();
            }
            if (tempGameData.fileSize == null || tempGameData.fileSize.isEmpty()) {
                tempGameData.fileSize = formatFileSize(dirSize(DocumentFile
                        .fromFile(tempGameData.gameDir)), controller.binaryPrefixes);
            }
            writeGameInfo(tempGameData, tempGameData.gameDir);
            if (tempPathFile != null) {
                copyFile(activityObservableField.get(), tempPathFile, tempGameData.gameDir);
            }
            if (tempModFile != null) {
                copyFile(activityObservableField.get(), tempModFile,
                        findFileRecursively(tempGameData.gameDir, "mods"));
            }
            refreshGamesDirectory();
            isShowDialog.set(false);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Error: " + ex);
        }
    }

    @SuppressLint("NonConstantResourceId")
    public void sendIntent(@NonNull View view) {
        Intent intentToStart;
        switch (view.getId()) {
            case R.id.buttonSelectArchive:
                Objects.requireNonNull(activityObservableField.get())
                        .showFilePickerDialog(new String[]{"application/zip", "application/rar"});
                break;
            case R.id.buttonSelectFolder:
                intentToStart = new Intent(ACTION_OPEN_DOCUMENT_TREE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    Objects.requireNonNull(activityObservableField.get())
                            .resultInstallDir.launch(intentToStart);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG , e.toString());
                }
                break;
            case R.id.buttonSelectIcon:
                intentToStart = new Intent(ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setType("*/*")
                        .putExtra(EXTRA_MIME_TYPES, new String[]{"image/png", "image/jpeg"});
                try {
                    Objects.requireNonNull(activityObservableField.get())
                            .resultGetImageLauncher.launch(
                                    Intent.createChooser(intentToStart , "Select an image"));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG , e.toString());
                }
                break;
            case R.id.buttonSelectPath:
                intentToStart = new Intent(ACTION_OPEN_DOCUMENT)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setType("application/octet-stream");
                try {
                    Objects.requireNonNull(activityObservableField.get())
                            .resultSetPath.launch(intentToStart);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.toString());
                }
                break;
            case R.id.buttonSelectMod:
                intentToStart = new Intent(ACTION_OPEN_DOCUMENT)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setType("application/octet-stream");
                try {
                    Objects.requireNonNull(activityObservableField.get())
                            .resultSetMod.launch(intentToStart);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.toString());
                }
                break;
        }
    }

    @NonNull
    private DialogInstallBinding formingInstallView() {
        installBinding =
                DialogInstallBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        installBinding.setStockVM(this);
        return installBinding;
    }

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding =
                DialogEditBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        editBinding.setStockVM(this);
        if (findFileRecursively(tempGameData.gameDir, "mods") != null) {
            editBinding.linearLayout4.setVisibility(View.VISIBLE);
        } else {
            editBinding.linearLayout4.setVisibility(View.GONE);
        }
        return editBinding;
    }
    // endregion Dialog

    // region Game install
    @NonNull
    public File getOrCreateGameDirectory(String gameName) {
        var folderName = normalizeFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    public void installGame(DocumentFile gameFile, GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Games directory is not writable");
            return;
        }
        try {
            doInstallGame(gameFile, gameData);
        } catch (InstallException ex) {
            if (Objects.requireNonNull(ex.getMessage()).equals("NIG")) {
                String message = getApplication()
                        .getString(R.string.installError)
                        .replace("-GAMENAME-", gameData.title);
                Objects.requireNonNull(activityObservableField.get()).showErrorDialog(message);
            } else if (ex.getMessage().equals("NFE")) {
                String message = getApplication()
                        .getString(R.string.noGameFilesError);
                Objects.requireNonNull(activityObservableField.get()).showErrorDialog(message);
            }
        }
    }

    private void doInstallGame(DocumentFile gameFile, GameData gameData) {
        var gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Games directory is not writable");
            return;
        }
        if (!gameFile.isDirectory()) {
            notificationStateGame(gameData.title);
        }
        var installer = new Installer(activityObservableField.get());
        installer.gameInstall(gameFile, gameDir).observeForever(aBoolean -> {
            if (aBoolean) {
                writeGameInfo(gameData, gameDir);
                refreshGames();
            }
        });
        isShowDialog.set(false);
    }

    private void notificationStateGame(String gameName) {
        var builder =
                new NotifyBuilder(activityObservableField.get(), "gameInstallationProgress");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.createDefaultChannel();
            builder.createProgressChannel();
        }
        builder.setTitleNotify(getApplication().getString(R.string.titleNotify));
        builder.setTextNotify(getApplication().getString(R.string.textProgressNotify));
        var notificationManager =
                NotificationManagerCompat.from(getApplication());
        if (ActivityCompat.checkSelfPermission(getApplication(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.buildWithProgress());
            ArchiveUtil.progressInstall.observeForever(aLong -> {
                notificationManager
                        .notify(1, builder.updateProgress((int) (aLong * 100 / ArchiveUtil.totalSize)));
                if (aLong == ArchiveUtil.totalSize) {
                    notificationManager.cancelAll();
                    var notifyBuilder =
                            new NotifyBuilder(activityObservableField.get(), "gameInstalled");
                    notifyBuilder.setTitleNotify(getApplication().getString(R.string.titleNotify));
                    var tempMessage = getApplication()
                            .getString(R.string.textInstallNotify)
                            .replace("-GAMENAME-", gameName);
                    notifyBuilder.setTextNotify(tempMessage);
                    notificationManager.notify(1, notifyBuilder.buildWithoutProgress());
                }
            });
        }
    }

    public void writeGameInfo(GameData gameData, File gameDir) {
        var infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Game data info file is not writable");
            return;
        }
        try (var out = new FileOutputStream(infoFile);
             var writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("Failed to write to a gameData info file");
        }
    }
    // endregion Game install

    // region Refresh
    public void refreshGamesDirectory() {
        var extFilesDir = getApplication().getExternalFilesDir(null);
        if (extFilesDir == null) {
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog("External files directory not found");
            return;
        }
        var dir = getOrCreateDirectory(extFilesDir, "games");
        if (!isWritableDirectory(dir)) {
            var message = "Games directory is not writable" + " " +
                    getApplication().getString(R.string.gamesDirError);
            Objects.requireNonNull(activityObservableField.get())
                    .showErrorDialog(message);
            return;
        }
        setGamesDir(dir);
        refreshGames();
    }

    public void refreshGames() {
        gamesMap.clear();
        for (var localGameData : localGame.getGames(gamesDir)) {
            var remoteGameData = gamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                var aggregateGameData = new GameData(remoteGameData);
                aggregateGameData.gameDir = localGameData.gameDir;
                aggregateGameData.gameFiles = localGameData.gameFiles;
                gamesMap.put(localGameData.id, aggregateGameData);
            } else {
                gamesMap.put(localGameData.id, localGameData);
            }
        }
        Objects.requireNonNull(activityObservableField.get()).setRecyclerAdapter();
    }
    // endregion Refresh
}

