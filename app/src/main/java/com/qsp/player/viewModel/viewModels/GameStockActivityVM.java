package com.qsp.player.viewModel.viewModels;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.qsp.player.utils.ArchiveUtil.progress;
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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;

import com.qsp.player.R;
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
    private DocumentFile tempInstallFile, tempImageFile;

    private DialogInstallBinding installBinding;

    public ObservableField<GameStockActivity> activityObservableField = new
            ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();

    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;

    // region Getter/Setter
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
    private AlertDialog dialog;

    public void showDialogInstall() {
       dialog = createAlertDialog(formingView());
       dialog.show();
       isShowDialog.set(true);
    }

    public void formingInstallIntent() {
        GameData gameData = new GameData();
        try {
            gameData.id = removeExtension(Objects.requireNonNull(tempInstallFile.getName()));
            gameData.title = (Objects.requireNonNull(
                    installBinding.installET0.getEditText()).getText().toString().isEmpty()?
                    removeExtension(Objects.requireNonNull(tempInstallFile.getName()))
                    : Objects.requireNonNull(
                            installBinding.installET0.getEditText()).getText().toString());
            gameData.author = (Objects.requireNonNull(
                    installBinding.installET1.getEditText()).getText().toString().isEmpty()?
                    null
                    : Objects.requireNonNull(
                            installBinding.installET1.getEditText()).getText().toString());
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

    public void sendIntent(View view) {
        String action;
        Intent intentInstall, intentGetImage;
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
                Log.e(TAG,e.toString());
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
        }
    }

    public void openGameDirectory () {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri data = FileProvider.getUriForFile(getApplication(), "com.qsp.player.provider", gamesDir);
        Log.d(TAG, data.toString());
        intent.setData(data);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        Objects.requireNonNull(activityObservableField.get())
                .startActivity(Intent.createChooser(intent, "Open folder"));
    }

    @NonNull
    private View formingView() {
        installBinding =
                DialogInstallBinding.inflate(Objects.requireNonNull(activityObservableField.get())
                        .getLayoutInflater());
        installBinding.setStockVM(this);
        return installBinding.getRoot();
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
                Objects.requireNonNull(activityObservableField.get()).onShowErrorDialog(message);
            } else if (ex.getMessage().equals("NFE")) {
                String message = getApplication()
                        .getString(R.string.noGameFilesError);
                Objects.requireNonNull(activityObservableField.get()).onShowErrorDialog(message);
            }
        }
    }

    private void doInstallGame(DocumentFile gameFile, GameData gameData) {
        File gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            Log.e(TAG, "Games directory is not writable");
            return;
        }
        notificationBuilder(gameFile);
        Installer installer = new Installer(activityObservableField.get());
        progress.observeForever(aLong -> {
            builder.setProgress((int) gameFile.length() , Math.toIntExact(aLong), false)
                    .setContentText(aLong + " of " + gameFile.length());
            notificationManager.notify(1, builder.build());
        });
        installer.gameInstall(gameFile, gameDir).observeForever(aBoolean -> {
            if (aBoolean) {
                writeGameInfo(gameData , gameDir);
                refreshGames();
                builder.setProgress(0, (int) gameFile.length(), false)
                        .setContentText("Completed");
                notificationManager.notify(1, builder.build());
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

    private void notificationBuilder (DocumentFile gameFile) {
        Intent resultIntent = new Intent(getApplication(), GameStockActivity.class);
        PendingIntent resultPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            resultPendingIntent = PendingIntent.getActivity(getApplication(), 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            resultPendingIntent = PendingIntent.getActivity(getApplication(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("qsp" , "QSP Channel" , importance);
            channel.setDescription("Reminders");
            notificationManager =
                    (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder =
                    new NotificationCompat.Builder(getApplication(), "myChannelId")
                            .setSmallIcon(R.drawable.download)
                            .setContentTitle("Install")
                            .setProgress((int) gameFile.length(), 0, true)
                            .setContentIntent(resultPendingIntent);
            notificationManager =
                    (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());
        } else {
            builder =
                    new NotificationCompat.Builder(Objects.requireNonNull(activityObservableField.get()))
                            .setSmallIcon(R.drawable.download)
                            .setContentTitle("Install")
                            .setProgress((int) gameFile.length(), 0, true)
                            .setContentIntent(resultPendingIntent);
            Notification notification = builder.build();
            notificationManager =
                    (NotificationManager) getApplication().getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(1, notification);
        }
    }

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

