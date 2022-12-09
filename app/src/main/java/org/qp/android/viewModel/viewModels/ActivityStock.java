package org.qp.android.viewModel.viewModels;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static org.qp.android.utils.FileUtil.GAME_INFO_FILENAME;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.createFile;
import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;
import static org.qp.android.utils.FileUtil.isWritableDirectory;
import static org.qp.android.utils.FileUtil.isWritableFile;
import static org.qp.android.utils.PathUtil.normalizeFolderName;
import static org.qp.android.utils.PathUtil.removeExtension;
import static org.qp.android.utils.XmlUtil.objectToXml;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.model.install.InstallException;
import org.qp.android.model.install.Installer;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.utils.ViewUtil;
import org.qp.android.view.stock.InstallDialogFragment;
import org.qp.android.view.stock.StockActivity;
import org.qp.android.viewModel.repository.LocalGame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Objects;

public class ActivityStock extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();

    public ObservableField<StockActivity> activityObservableField = new
            ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();

    private final LocalGame localGame = new LocalGame();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private DocumentFile tempInstallFile, tempImageFile, tempPathFile;

    private DialogInstallBinding installBinding;
    private GameData tempGameData;
    private DialogEditBinding editBinding;

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
        if (installBinding != null) {
            installBinding.imageTV.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(installBinding.imageView);
        } else {
            editBinding.imageTV.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(editBinding.imageView);
        }
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
    private final InstallDialogFragment dialogFragment = new InstallDialogFragment();

    public void showDialogInstall() {
       dialogFragment.setInstallBinding(formingInstallView());
       dialogFragment.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
       Objects.requireNonNull(activityObservableField.get())
               .showDialogFragment(dialogFragment);
       isShowDialog.set(true);
    }

    public void createInstallIntent() {
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
            dialogFragment.dismiss();
        } catch (NullPointerException ex) {
            Log.e(TAG, "Error: ", ex);
        }
    }

    public void showDialogEdit (GameData gameData) {
        tempGameData = gameData;
        dialogFragment.setEditBinding(formingEditView());
        dialogFragment.onCancel(new DialogInterface() {
            @Override
            public void cancel() {
                isShowDialog.set(false);
            }

            @Override
            public void dismiss() {
            }
        });
        Objects.requireNonNull(activityObservableField.get())
                .showDialogFragment(dialogFragment);
        isShowDialog.set(true);
    }

    public void createEditIntent() {
        try {
            tempGameData.title = (Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.title)
                    : Objects.requireNonNull(
                    editBinding.ET0.getEditText()).getText().toString());
            tempGameData.author = (Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.author)
                    : Objects.requireNonNull(
                    editBinding.ET1.getEditText()).getText().toString());
            tempGameData.version = (Objects.requireNonNull(
                    editBinding.installET2.getEditText()).getText().toString().isEmpty()?
                    removeExtension(tempGameData.version)
                    : Objects.requireNonNull(
                    editBinding.installET2.getEditText()).getText().toString());
            tempGameData.icon = (tempImageFile == null ? tempGameData.icon
                    : tempImageFile.getUri().toString());
            writeGameInfo(tempGameData, tempGameData.gameDir);
            if (tempPathFile != null) {
                copyFile(activityObservableField.get(), tempPathFile, tempGameData.gameDir);
            }
            refreshGamesDirectory();
            isShowDialog.set(false);
            dialogFragment.dismiss();
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
            intentInstall.addCategory(Intent.CATEGORY_OPENABLE);
            intentInstall.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentInstall.setType("*/*");
            String[] mimeTypes = {"application/zip", "application/rar"};
            intentInstall.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultInstallLauncher.launch(
                                Intent.createChooser(intentInstall, "Select an archive"));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG , e.toString());
            }
        } else if (id == R.id.button3) {
            action = ACTION_OPEN_DOCUMENT;
            intentGetImage = new Intent(action);
            intentGetImage.addCategory(Intent.CATEGORY_OPENABLE);
            intentGetImage.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentGetImage.setType("*/*");
            String[] mimeTypes = {"image/png", "image/jpeg"};
            intentGetImage.putExtra(EXTRA_MIME_TYPES, mimeTypes);
            try {
                Objects.requireNonNull(activityObservableField.get())
                        .resultGetImageLauncher.launch(
                                Intent.createChooser(intentGetImage, "Select an image"));
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
        return editBinding;
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
                notifyGameWasInstall();
                writeGameInfo(gameData , gameDir);
                refreshGames();
            }
        });
        isShowDialog.set(false);
    }

    private void notifyGameWasInstall () {
        NotifyBuilder builder =
                new NotifyBuilder(activityObservableField.get(), "org.qp.android");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.createChannel();
        }
        builder.setTitleNotify("Game install");
        builder.setTextNotify("The game has been successfully installed!");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplication());
        notificationManager.notify(1, builder.build());
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
        for (GameData localGameData : localGame.getGames(gamesDir)) {
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

