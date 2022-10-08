package com.qsp.player.viewModel.viewModels;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static com.qsp.player.utils.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.isWritableDirectory;
import static com.qsp.player.utils.FileUtil.isWritableFile;
import static com.qsp.player.utils.PathUtil.normalizeFolderName;
import static com.qsp.player.utils.PathUtil.removeExtension;
import static com.qsp.player.utils.XmlUtil.objectToXml;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;

import com.qsp.player.R;
import com.qsp.player.databinding.DialogEditBinding;
import com.qsp.player.databinding.DialogInstallBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.model.install.InstallException;
import com.qsp.player.model.install.Installer;
import com.qsp.player.utils.ViewUtil;
import com.qsp.player.view.activities.GameStockActivity;
import com.qsp.player.viewModel.repository.LocalGameRepository;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Objects;

public class GameStockActivityVM extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();

    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private DocumentFile tempInstallFile, tempImageFile, tempPathFile;

    private DialogInstallBinding installBinding;
    private GameData tempGameData;
    private DialogEditBinding editBinding;

    public ObservableField<GameStockActivity> activityObservableField = new
            ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();
    private AlertDialog dialog;

    // region Getter/Setter
    public void setTempPathFile(DocumentFile tempPathFile) {
        this.tempPathFile = tempPathFile;
        editBinding.fileTV.setText(tempPathFile.getName());
    }

    public void setTempInstallFile(@NonNull DocumentFile tempInstallFile) {
        this.tempInstallFile = tempInstallFile;
        installBinding.fileTV.setText(tempInstallFile.getName());
    }

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
        installBinding.imageTV.setText(tempImageFile.getName());
        Picasso.get()
                .load(tempImageFile.getUri())
                .fit()
                .into(installBinding.imageView);
    }

    public void setGamesDir(File gamesDir) {
        this.gamesDir = gamesDir;
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }
    // endregion Getter/Setter

    public GameStockActivityVM(@NonNull Application application) {
        super(application);
    }

    // region Dialog
    public void showDialogInstall() {
       dialog = createAlertDialog(formingInstallView());
       dialog.show();
       isShowDialog.set(true);
    }

    public void formingInstallIntent() {
        GameData gameData = new GameData();
        try {
            gameData.id = removeExtension(Objects.requireNonNull(tempInstallFile.getName()));
            gameData.title = (Objects.requireNonNull(
                    installBinding.ET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(Objects.requireNonNull(tempInstallFile.getName()))
                    : Objects.requireNonNull(
                            installBinding.ET0.getEditText()).getText().toString());
            gameData.author = (Objects.requireNonNull(
                    installBinding.ET1.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                            installBinding.ET1.getEditText()).getText().toString());
            gameData.version = (Objects.requireNonNull(
                    installBinding.installET2.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                            installBinding.installET2.getEditText()).getText().toString());
            gameData.fileSize = String.valueOf(tempInstallFile.length() / 1000);
            gameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
            installGame(tempInstallFile, gameData);
            dialog.dismiss();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Error: ", ex);
        }
    }

    public void showDialogEdit (GameData gameData) {
        tempGameData = gameData;
        dialog = createAlertDialog(formingEditView());
        dialog.show();
        isShowDialog.set(true);
    }

    public void formingEditIntent() {
        try {
            tempGameData.title = (Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(Objects.requireNonNull(tempGameData.title))
                    : Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString());
            tempGameData.author = (Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString());
            tempGameData.version = (Objects.requireNonNull(
                    editBinding.installET2.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                    editBinding.installET2.getEditText()).getText().toString());
            tempGameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
            writeGameInfo(tempGameData, tempGameData.gameDir);
            if (tempPathFile != null) {
                Installer installer = new Installer(activityObservableField.get());
                installer.copyFileOrDirectory(tempPathFile, tempGameData.gameDir);
            }
            refreshGamesDirectory();
            isShowDialog.set(false);
            dialog.dismiss();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Error: ", ex);
        }
    }

    public void sendIntent(View view) {
        String action;
        Intent intentInstall, intentGetImage, intentSetPath;
        int id = view.getId();
        if (id == R.id.button2) {
            action = ACTION_OPEN_DOCUMENT;
            intentInstall = new Intent(action);
            intentInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentInstall.setType("*/*");
            String[] mimeTypes = {"application/zip", "application/rar"};
            intentInstall.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultInstallLauncher.launch(intentInstall);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.button3) {
            action = ACTION_OPEN_DOCUMENT;
            intentGetImage = new Intent(action);
            intentGetImage.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentGetImage.setType("*/*");
            String[] mimeTypes = {"image/png", "image/jpeg"};
            intentGetImage.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultGetImageLauncher.launch(intentGetImage);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.buttonSelectPath) {
            action = ACTION_OPEN_DOCUMENT;
            intentSetPath = new Intent(action);
            intentSetPath.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentSetPath.setType("application/octet-stream");
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultSetPath.launch(intentSetPath);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        }
    }

    @NonNull
    private View formingInstallView() {
        installBinding =
                DialogInstallBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        installBinding.setStockVM(this);
        return installBinding.getRoot();
    }

    private View formingEditView() {
        editBinding =
                DialogEditBinding.inflate(LayoutInflater.from(activityObservableField.get()));
        editBinding.setStockVM(this);
        return editBinding.getRoot();
    }

    private AlertDialog createAlertDialog (View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activityObservableField.get());
        dialogBuilder.setOnCancelListener(dialogInterface -> isShowDialog.set(false));
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }
    // endregion Dialog

    // region Game install
    @NonNull
    public File getOrCreateGameDirectory(String gameName) {
        String folderName = normalizeFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    public void installGame(DocumentFile gameFile, GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            Log.e(TAG, "Games directory is not writable");
            return;
        }
        try {
            doInstallGame(gameFile, gameData);
        } catch (InstallException ex) {
            Log.e(TAG, ex.getMessage());
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
        File gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            Log.e(TAG, "Games directory is not writable");
            return;
        }
        Installer installer = new Installer(activityObservableField.get());
        installer.gameInstall(gameFile, gameDir).observeForever(aBoolean -> {
            if (aBoolean) {
                writeGameInfo(gameData , gameDir);
                refreshGames();
            }
        });
        isShowDialog.set(false);
    }

    public void writeGameInfo(GameData gameData , File gameDir) {
        File infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            Log.e(TAG, "Game data info file is not writable");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(infoFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            Log.e(TAG,"Failed to write to a gameData info file", ex);
        }
    }
    // endregion Game install

    // region Refresh
    public void refreshGamesDirectory() {
        File extFilesDir = getApplication().getExternalFilesDir(null);
        if (extFilesDir == null) {
            Log.e(TAG,"External files directory not found");
            return;
        }
        File dir = getOrCreateDirectory(extFilesDir, "games");
        if (!isWritableDirectory(dir)) {
            Log.e(TAG,"Games directory is not writable");
            String message = getApplication().getString(R.string.gamesDirError);
            ViewUtil.showErrorDialog(getApplication(), message);
            return;
        }
        setGamesDir(dir);
        refreshGames();
    }

    public void refreshGames() {
        gamesMap.clear();
        for (GameData localGameData : localGameRepository.getGames(gamesDir)) {
            GameData remoteGameData = gamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                GameData aggregateGameData = new GameData(remoteGameData);
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

