package org.qp.android.viewModel;

import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.createFindDFile;
import static org.qp.android.utils.FileUtil.createFindFile;
import static org.qp.android.utils.FileUtil.createFindFolder;
import static org.qp.android.utils.FileUtil.dirSize;
import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.findFileRecursively;
import static org.qp.android.utils.FileUtil.formatFileSize;
import static org.qp.android.utils.FileUtil.isWritableDirectory;
import static org.qp.android.utils.FileUtil.isWritableFile;
import static org.qp.android.utils.PathUtil.normalizeFolderName;
import static org.qp.android.utils.PathUtil.removeExtension;
import static org.qp.android.utils.XmlUtil.objectToXml;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.file.MimeType;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.qp.android.GameDataParcel;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.databinding.DialogInstallBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.model.install.Installer;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginType;
import org.qp.android.plugin.AsyncCallback;
import org.qp.android.utils.SerializeObject;
import org.qp.android.view.game.GameActivity;
import org.qp.android.view.settings.SettingsController;
import org.qp.android.view.stock.StockActivity;
import org.qp.android.view.stock.fragment.dialogs.StockDialogFrags;
import org.qp.android.view.stock.fragment.dialogs.StockDialogType;
import org.qp.android.viewModel.repository.LocalGame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class StockViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();
    private static final String GAME_INFO_FILENAME = "gameStockInfo";

    public ObservableField<StockActivity> activityObservableField =
            new ObservableField<>();

    public ObservableBoolean isShowDialog = new ObservableBoolean();
    public ObservableBoolean isSelectFolder = new ObservableBoolean();

    private final LocalGame localGame = new LocalGame();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private DocumentFile tempInstallDir, tempImageFile, tempPathFile, tempModFile;

    private DialogInstallBinding installBinding;
    private GameData tempGameData;
    private DialogEditBinding editBinding;
    private SettingsController controller;

    private final MutableLiveData<ArrayList<GameData>> gameDataList;

    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();

    // region Getter/Setter
    public void setController(SettingsController controller) {
        this.controller = controller;
    }

    public void setTempGameData(GameData tempGameData) {
        this.tempGameData = tempGameData;
    }

    public void setTempPathFile(DocumentFile tempPathFile) {
        this.tempPathFile = tempPathFile;
        if (editBinding == null) return;
        editBinding.buttonSelectPath.setText(tempPathFile.getName());
    }

    public void setTempModFile(DocumentFile tempModFile) {
        this.tempModFile = tempModFile;
        if (editBinding == null) return;
        editBinding.buttonSelectMod.setText(tempModFile.getName());
    }

    public void setTempInstallDir(@NonNull DocumentFile tempInstallDir) {
        this.tempInstallDir = tempInstallDir;
        if (installBinding == null) return;
        installBinding.buttonSelectFolder.setText(tempInstallDir.getName());
    }

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
        if (installBinding != null) {
            installBinding.buttonSelectIcon.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(installBinding.imageView);
        }
        if (editBinding != null) {
            editBinding.buttonSelectIcon.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(editBinding.imageView);
        }
    }

    public void setGamesDir(File gamesDir) {
        this.gamesDir = gamesDir;
    }

    public void setGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
    }

    public void setLocalGameDataList () {
        var gameData = getSortedGames();
        var localGameData = new ArrayList<GameData>();
        for (var data : gameData) {
            if (data.isInstalled()) {
                localGameData.add(data);
            }
        }
        setGameDataList(localGameData);
    }

    @NotNull
    private StockActivity getStockActivity() {
        var tempStockActivity = activityObservableField.get();
        if (tempStockActivity != null) {
            return tempStockActivity;
        } else {
            throw new NullPointerException();
        }
    }

    public String getGameIdByPosition(int position) {
        getGameData().observeForever(gameDataArrayList -> {
            if (!gameDataArrayList.isEmpty() && gameDataArrayList.size() > position) {
                setTempGameData(gameDataArrayList.get(position));
            }
        });
        return getTempGameData().id;
    }

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);
        if (gameData.size() < 2) return gameData;
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase()));
        return gameData;
    }

    public LiveData<ArrayList<GameData>> getGameData() {
        if (gameDataList.getValue() != null) {
            var application = (QuestPlayerApplication) getApplication();
            application.setGameList(gameDataList.getValue());
        }
        return gameDataList;
    }

    public GameData getTempGameData() {
        return tempGameData;
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameAuthor () {
        if (tempGameData.author.length() > 0) {
            return getStockActivity()
                    .getString(R.string.author)
                    .replace("-AUTHOR-" , tempGameData.author);
        } else {
            return "";
        }
    }

    public String getGamePortBy () {
        if (tempGameData.portedBy.length() > 0) {
            return getStockActivity()
                    .getString(R.string.ported_by)
                    .replace("-PORTED_BY-", tempGameData.portedBy);
        } else {
            return "";
        }
    }

    public String getGameVersion () {
        if (tempGameData.version.length() > 0) {
            return getStockActivity()
                    .getString(R.string.version)
                    .replace("-VERSION-" , tempGameData.version);
        } else {
            return "";
        }
    }

    public String getGameType () {
        if (tempGameData.fileExt.length() > 0) {
            if (tempGameData.fileExt.equals("aqsp")) {
                return getStockActivity()
                        .getString(R.string.fileType)
                        .replace("-TYPE-", tempGameData.fileExt)
                        + " " + getStockActivity().getString(R.string.experimental);
            }
            return getStockActivity()
                    .getString(R.string.fileType)
                    .replace("-TYPE-", tempGameData.fileExt);
        } else {
            return "";
        }
    }

    public String getGameSize () {
        if (tempGameData.getFileSize() != null) {
            return getStockActivity()
                    .getString(R.string.fileSize)
                    .replace("-SIZE-" , tempGameData.getFileSize());
        } else {
            return "";
        }
    }

    public String getGamePubData () {
        if (tempGameData.pubDate.length() > 0) {
            return getStockActivity()
                    .getString(R.string.pub_data)
                    .replace("-PUB_DATA-", tempGameData.pubDate);
        } else {
            return "";
        }
    }

    public String getGameModData () {
        if (tempGameData.modDate.length() > 0) {
            return getStockActivity()
                    .getString(R.string.mod_data)
                    .replace("-MOD_DATA-", tempGameData.pubDate);
        } else {
            return "";
        }
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled () {
        return tempGameData.isInstalled() && doesDirectoryContainGameFiles(tempGameData.gameDir);
    }

    public boolean isHasRemoteUrl () {
        return tempGameData.hasRemoteUrl();
    }

    public boolean isModsDirExist() {
        return findFileRecursively(tempGameData.gameDir, "mods") != null;
    }
    // endregion Getter/Setter

    public StockViewModel(@NonNull Application application) {
        super(application);
        gameDataList = new MutableLiveData<>();
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
        getStockActivity()
                .showInstallDialogFragment(dialogFragments);
        isShowDialog.set(true);
    }

    public void createInstallIntent() {
        var gameData = new GameData();
        try {
            if (tempInstallDir != null && tempInstallDir.getName() != null) {
                gameData.id = removeExtension(tempInstallDir.getName());
                var installTextTitle = installBinding.ET0.getEditText();
                if (installTextTitle != null) {
                    gameData.title = installTextTitle.getText().toString().isEmpty()
                            ? removeExtension(tempInstallDir.getName())
                            : installTextTitle.getText().toString();
                }
                var installTextAuthor = installBinding.ET1.getEditText();
                if (installTextAuthor != null) {
                    gameData.author = installTextAuthor.getText().toString().isEmpty()
                            ? null
                            : installTextAuthor.getText().toString();
                }
                var installTextVersion = installBinding.ET2.getEditText();
                if (installTextVersion != null) {
                    gameData.version = installTextVersion.getText().toString().isEmpty()
                            ? null
                            : installTextVersion.getText().toString();
                }
                gameData.fileSize = formatFileSize(dirSize(tempInstallDir) , controller.binaryPrefixes);
                gameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
                installGame(tempInstallDir , gameData);
                isSelectFolder.set(false);
                dialogFragments.dismiss();
            }
        } catch (NullPointerException ex) {
            Log.d(TAG , "Error: " , ex);
        }
    }

    public void createUseIntent() {
        var gameData = new GameData();
        try {
            if (tempInstallDir != null && tempInstallDir.getName() != null) {
                gameData.id = removeExtension(tempInstallDir.getName());
                var installTextTitle = installBinding.ET0.getEditText();
                if (installTextTitle != null) {
                    gameData.title = installTextTitle.getText().toString().isEmpty()
                            ? removeExtension(tempInstallDir.getName())
                            : installTextTitle.getText().toString();
                }
                var installTextAuthor = installBinding.ET1.getEditText();
                if (installTextAuthor != null) {
                    gameData.author = installTextAuthor.getText().toString().isEmpty()
                            ? null
                            : installTextAuthor.getText().toString();
                }
                var installTextVersion = installBinding.ET2.getEditText();
                if (installTextVersion != null) {
                    gameData.version = installTextVersion.getText().toString().isEmpty()
                            ? null
                            : installTextVersion.getText().toString();
                }
                gameData.fileSize = formatFileSize(dirSize(tempInstallDir) , controller.binaryPrefixes);
                gameData.icon = (tempImageFile == null ? null : tempImageFile.getUri().toString());
                connectingDir(tempInstallDir , gameData);
                isSelectFolder.set(false);
                dialogFragments.dismiss();
            }
        } catch (NullPointerException ex) {
            Log.d(TAG , "Error: " , ex);
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
        getStockActivity()
                .showEditDialogFragment(dialogFragments);
        isShowDialog.set(true);
    }

    public void playGame() {
        var intent = new Intent(getStockActivity() ,
                GameActivity.class);
        intent.putExtra("gameId" , tempGameData.id);
        intent.putExtra("gameTitle" , tempGameData.title);
        intent.putExtra("gameDirUri" , tempGameData.gameDir.getAbsolutePath());
        var gameFileCount = tempGameData.gameFiles.size();
        switch (gameFileCount) {
            case 0:
                getStockActivity()
                        .showErrorDialog("Game folder has no game files!");
                break;
            case 1:
                intent.putExtra("gameFileUri" , tempGameData.gameFiles.get(0).getAbsolutePath());
                getStockActivity().startGameActivity(intent);
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
                getStockActivity()
                        .showSelectDialogFragment(dialogFragments);
                outputIntObserver.observeForever(integer -> {
                    intent.putExtra("gameFileUri" ,
                            tempGameData.gameFiles.get(integer).getAbsolutePath());
                    getStockActivity().startGameActivity(intent);
                });
                break;
        }
    }

    public void createEditIntent() {
        try {
            var editTextTitle = editBinding.ET0.getEditText();
            if (editTextTitle != null) {
                tempGameData.title = editTextTitle.getText().toString().isEmpty()
                        ? removeExtension(tempGameData.title)
                        : editTextTitle.getText().toString();
            }
            var editTextAuthor = editBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                tempGameData.author = editTextAuthor.getText().toString().isEmpty()
                        ? removeExtension(tempGameData.author)
                        : editTextAuthor.getText().toString();
            }
            var editTextVersion = editBinding.ET2.getEditText();
            if (editTextVersion != null) {
                tempGameData.version = editTextVersion.toString().isEmpty()
                        ? removeExtension(tempGameData.version)
                        : editTextVersion.getText().toString();
            }
            if (tempImageFile != null) tempGameData.icon = tempImageFile.getUri().toString();
            if (tempGameData.fileSize == null || tempGameData.fileSize.isEmpty()) {
                tempGameData.fileSize = formatFileSize(dirSize(DocumentFile
                                .fromFile(tempGameData.gameDir)) , controller.binaryPrefixes);
            }
            writeGameInfo(tempGameData , tempGameData.gameDir);
            if (tempPathFile != null) {
                copyFile(getStockActivity() , tempPathFile , tempGameData.gameDir);
            }
            if (tempModFile != null) {
                copyFile(getStockActivity() , tempModFile ,
                        findFileRecursively(tempGameData.gameDir, "mods"));
            }
            refreshIntGamesDirectory();
            isShowDialog.set(false);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            getStockActivity().showErrorDialog("Error: " + ex);
        }
    }

    public void sendIntent(@NonNull View view) {
        int id = view.getId();
        if (id == R.id.buttonSelectFolder) {
            getStockActivity()
                    .showDirPickerDialog();
        } else if (id == R.id.buttonSelectIcon) {
            getStockActivity()
                    .showFilePickerActivity(new String[]{"image/png" , "image/jpeg"});
        } else if (id == R.id.buttonSelectPath) {
            getStockActivity()
                    .showFilePickerActivity(new String[]{"application/octet-stream"});
        } else if (id == R.id.buttonSelectMod) {
            getStockActivity()
                    .showFilePickerActivity(new String[]{"application/octet-stream"});
        }
    }

    @NonNull
    private DialogInstallBinding formingInstallView() {
        installBinding =
                DialogInstallBinding.inflate(LayoutInflater.from(getStockActivity()));
        installBinding.setStockVM(this);
        installBinding.buttonSelectFolder.setOnClickListener(v ->
                sendIntent(installBinding.buttonSelectFolder));
        installBinding.buttonSelectIcon.setOnClickListener(v ->
                sendIntent(installBinding.buttonSelectIcon));
        installBinding.installBT.setOnClickListener(v ->
                createInstallIntent());
        installBinding.useBT.setOnClickListener(v ->
                createUseIntent());
        return installBinding;
    }

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding =
                DialogEditBinding.inflate(LayoutInflater.from(getStockActivity()));
        editBinding.setStockVM(this);

        if (!tempGameData.icon.isEmpty()) {
            Picasso.get()
                    .load(tempGameData.icon)
                    .fit()
                    .into(editBinding.imageView);
        }

        editBinding.buttonSelectPath.setOnClickListener(this::sendIntent);
        editBinding.buttonSelectMod.setOnClickListener(this::sendIntent);
        editBinding.buttonSelectIcon.setOnClickListener(this::sendIntent);
        editBinding.editBT.setOnClickListener(v -> createEditIntent());
        return editBinding;
    }
    // endregion Dialog

    // region Game connecting
    public void connectingDir(DocumentFile gameFile , GameData gameData) {
        writeGameInfo(gameData , gameFile);
        FileWrapper.Document fos = new FileWrapper.Document(gameFile);
        var file = new File(fos.getAbsolutePath(getApplication()));
        Log.d(TAG , file.getAbsolutePath());
        refreshExtGameDirectory(file);
    }

    public void writeGameInfo(GameData gameData , DocumentFile gameDir) {
        var infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFindDFile(gameDir, MimeType.TEXT , GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            getStockActivity()
                    .showErrorDialog("Game data info file is not writable");
            return;
        }
        try (var out = getStockActivity().getContentResolver()
                .openOutputStream(infoFile.getUri(), "w");
             var writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            Log.d(TAG , "Error: " , ex);
        }
    }
    // endregion Game connecting

    // region Game install
    public void installGame(DocumentFile gameFile , GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            getStockActivity()
                    .showErrorDialog("Games directory is not writable");
            return;
        }
        doInstallGame(gameFile , gameData);
    }

    private void doInstallGame(DocumentFile gameFile , GameData gameData) {
        var gameDir = createFindFolder(gamesDir , normalizeFolderName(gameData.title));
        if (!isWritableDirectory(gameDir)) {
            getStockActivity().showErrorDialog("Games directory is not writable");
            return;
        }

        isShowDialog.set(false);

        var installer = new Installer(getStockActivity());
        installer.getErrorCode().observeForever(error -> {
            switch (error) {
                case "NIG":
                    getStockActivity().showErrorDialog(getStockActivity()
                            .getString(R.string.installError)
                            .replace("-GAMENAME-" , gameData.title));
                    break;
                case "NFE":
                    getStockActivity().showErrorDialog(getStockActivity()
                            .getString(R.string.noGameFilesError));
                    break;
            }
        });

        installer.gameInstall(gameFile , gameDir).observeForever(aBoolean -> {
            if (aBoolean) {
                writeGameInfo(gameData , gameDir);
                refreshGameData();
            }
        });
    }

    public void writeGameInfo(GameData gameData , File gameDir) {
        var infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFindFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            getStockActivity()
                    .showErrorDialog("Game data info file is not writable");
            return;
        }
        try (var out = new FileOutputStream(infoFile);
             var writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            getStockActivity()
                    .showErrorDialog("Failed to write to a gameData info file");
        }
    }
    // endregion Game install

    // region Refresh
    public void refreshIntGamesDirectory() {
        var intFilesDir = getApplication().getExternalFilesDir(null);
        if (intFilesDir == null) {
            getStockActivity()
                    .showErrorDialog("Internal files directory not found");
            return;
        }
        var tempGameDir = createFindFolder(intFilesDir, "games");
        if (!isWritableDirectory(tempGameDir)) {
            var message = "Games directory is not writable" + " " +
                    getStockActivity().getString(R.string.gamesDirError);
            getStockActivity().showErrorDialog(message);
            return;
        }
        setGamesDir(tempGameDir);
        refreshGameData();
    }

    public void refreshExtGameDirectory(File gamesDir) {
        for (var localGameData : localGame.getGame(gamesDir)) {
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
        setLocalGameDataList();
    }

    public void refreshGameData() {
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

        var restoreList = restoreGameLists();
        if (restoreList != null) {
            restoreList.removeIf(data -> !data.gameDir.exists());
            refreshGames(restoreList);
        }

        setLocalGameDataList();
    }

    public void saveGameLists(ArrayList<GameData> inputListGameData) {
        var tempSaveGameList = createFindFile(getStockActivity().getCacheDir() , "tempGameLists");
        if (tempSaveGameList == null) {
            try {
                tempSaveGameList = File.createTempFile("tempGameLists" , null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (isWritableFile(tempSaveGameList)) {
            var ser = SerializeObject.objectToString(inputListGameData);
            if (ser != null && !ser.equalsIgnoreCase("")) {
                SerializeObject.WriteSettings(getStockActivity(), ser, "tempGameLists");
            } else {
                SerializeObject.WriteSettings(getStockActivity(), "", "tempGameLists");
            }
        }
    }

    @Nullable
    public ArrayList<GameData> restoreGameLists() {
        var tempSaveGameList = createFindFile(getStockActivity().getCacheDir() , "tempGameLists");
        var tempDataList = new ArrayList<GameData>();
        if (isWritableFile(tempSaveGameList)) {
            var ser = SerializeObject.ReadSettings(getStockActivity(), "tempGameLists");
            if (ser != null && !ser.equalsIgnoreCase("")) {
                Object obj = SerializeObject.stringToObject(ser);
                if (obj instanceof ArrayList) {
                    tempDataList = (ArrayList<GameData>) obj;
                }
            }
        }
        return tempDataList;
    }

    public void refreshGames(ArrayList<GameData> gameDataArrayList) {
        gamesMap.clear();
        for (var localGameData : gameDataArrayList) {
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

        var tempGamesMap = new HashMap<String, GameData>();
        for (var localGameData : localGame.getGames(gamesDir)) {
            var remoteGameData = tempGamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                var aggregateGameData = new GameData(remoteGameData);
                aggregateGameData.gameDir = localGameData.gameDir;
                aggregateGameData.gameFiles = localGameData.gameFiles;
                tempGamesMap.put(localGameData.id, aggregateGameData);
            } else {
                tempGamesMap.put(localGameData.id, localGameData);
            }
        }
        gamesMap.putAll(tempGamesMap);
        setLocalGameDataList();
    }
    // endregion Refresh

    // region Plugin
    public boolean isDownloadPlugin () {
        var pluginClient = new PluginClient();
        pluginClient.loadListPlugin(getApplication());
        return pluginClient.getNamePlugin() != null
                && pluginClient.getNamePlugin().equals("org.qp.android.plugin.AidlService");
    }

    public void startDownloadPlugin () {
        var pluginClient = new PluginClient();
        pluginClient.loadListPlugin(getApplication());
        pluginClient.connectPlugin(getApplication() , PluginType.DOWNLOAD_PLUGIN);
        new Handler().postDelayed(() -> {
            try {
                pluginClient.getQuestPlugin().arrayGameData(new AsyncCallback.Stub() {
                    @Override
                    public void onSuccess(List<GameDataParcel> gameDataParcel) throws RemoteException {
                        refreshGames(convertDTO(gameDataParcel));
                        setGameDataList(convertDTO(gameDataParcel));
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG , "Error: " , e);
            }
        } , 1000);
    }

    @NonNull
    private ArrayList<GameData> convertDTO (List<GameDataParcel> gameDataParcelList) {
        var gameDataArrayList = new ArrayList<GameData>();
        for (var gameDataParcel : gameDataParcelList) {
            var gameData = new GameData();
            gameData.id = gameDataParcel.id;
            gameData.listId = gameDataParcel.listId;
            gameData.author = gameDataParcel.author;
            gameData.portedBy = gameDataParcel.portedBy;
            gameData.version = gameDataParcel.version;
            gameData.title = gameDataParcel.title;
            gameData.lang = gameDataParcel.lang;
            gameData.player = gameDataParcel.player;
            gameData.icon = gameDataParcel.icon;
            gameData.fileUrl = gameDataParcel.fileUrl;
            gameData.fileSize = gameDataParcel.fileSize;
            gameData.fileExt = gameDataParcel.fileExt;
            gameData.descUrl = gameDataParcel.descUrl;
            gameData.pubDate = gameDataParcel.pubDate;
            gameData.modDate = gameDataParcel.modDate;
            gameData.gameDir = gameDataParcel.gameDir;
            gameData.gameFiles = gameDataParcel.gameFiles;
            gameDataArrayList.add(gameData);
        }
        return gameDataArrayList;
    }
    // endregion Plugin
}

