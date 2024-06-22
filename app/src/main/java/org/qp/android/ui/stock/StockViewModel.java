package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.FileUtil.forceDelFile;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.data.db.GameDatabase;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.EventEmitter;
import org.qp.android.helpers.repository.RepositoryLocal;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StockViewModel extends AndroidViewModel {

    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public static final String DISABLE_CALCULATE_DIR = "disable";

    public MutableLiveData<StockActivity> activityObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;

    // Containers
    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final MutableLiveData<ArrayList<GameData>> gameDataList = new MutableLiveData<>();

    private final RepositoryLocal localGame;
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private GameData currGameData;
    private final MutableLiveData<GameData> gameLiveData = new MutableLiveData<>();
    private DialogEditBinding editBinding;
    private DialogAddBinding addBinding;
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    private final GameDatabase gameDatabase;
    private final GameDao gameDao;

    public EventEmitter emitter = new EventEmitter();


    // region Getter/Setter
    public void setCurrGameData(GameData currGameData) {
        gameLiveData.setValue(currGameData);
        this.currGameData = currGameData;
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

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
        if (editBinding != null) {
            editBinding.buttonSelectIcon.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(editBinding.imageView);
        }
    }

    public void setValueGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.setValue(gameDataArrayList);
    }

    public void postValueGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
    }

    public LiveData<GameData> getGameLiveData() {
        return gameLiveData;
    }

    @NotNull
    private StockActivity getStockActivity() {
        var tempStockActivity = activityObserver.getValue();
        if (tempStockActivity != null) {
            return tempStockActivity;
        } else {
            throw new NullPointerException();
        }
    }

    private SettingsController getController() {
        return SettingsController.newInstance(getApplication());
    }

    public ArrayList<GameData> getListGames() {
        return new ArrayList<>(gamesMap.values());
    }

    public LiveData<ArrayList<GameData>> getGameDataList() {
        return gameDataList;
    }

    public Optional<GameData> getCurrGameData() {
        return Optional.ofNullable(currGameData);
    }

    public int getCountGameFiles() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            return data.gameFiles.size();
        } else {
            return 0;
        }
    }

    @Nullable
    public DocumentFile getGameFile(int index) {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            return data.gameFiles.get(index);
        } else {
            return null;
        }
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled() {
        if (getCurrGameData().isPresent()) {
            var gameData = getCurrGameData().get();
            return gameData.isFileInstalled() && isDirContainsGameFile(gameData.gameDir);
        } else {
            return false;
        }
    }

    public boolean isHasRemoteUrl() {
        if (getCurrGameData().isPresent()) {
            return getCurrGameData().get().isHasRemoteUrl();
        } else {
            return false;
        }
    }

    public boolean isModsDirExist() {
        if (getCurrGameData().isPresent()) {
            var gameData = getCurrGameData().get();
            return gameData.gameDir.findFile("mods") != null;
        } else {
            return false;
        }
    }
    // endregion Getter/Setter

    public void doOnShowFilePicker(int requestCode , String[] mimeTypes) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowFilePicker(requestCode , mimeTypes));
    }

    public void doOnShowErrorDialog(String errorMessage , ErrorType errorType) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowErrorDialog(errorMessage , errorType));
    }

    public void doOnShowGameFragment(int position) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowGameFragment(position));
    }

    public void doOnShowActionMode() {
        emitter.emitAndExecute(new StockFragmentNavigation.ShowActionMode());
    }

    @Inject
    public StockViewModel(@NonNull Application application,
                          GameDao gameDao,
                          GameDatabase gameDatabase) {
        super(application);

        this.gameDao = gameDao;
        this.gameDatabase = gameDatabase;

        localGame = new RepositoryLocal(gameDao, getApplication());
    }

    // region Dialog
    public void showAddDialogFragment(FragmentManager manager,
                                      DocumentFile rootDir) {
        outputIntObserver = new MutableLiveData<>();
        dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.ADD_DIALOG);
        dialogFragments.setAddBinding(setupAddView(rootDir));
        dialogFragments.show(manager , "addDialogFragment");
    }

    public void showDialogFragment(FragmentManager manager ,
                                   StockDialogType dialogType ,
                                   String errorMessage) {
        var fragment = manager.findFragmentByTag(dialogFragments.getTag());
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            switch (dialogType) {
                case DELETE_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setMessage(errorMessage);
                    dialogFragments.setDialogType(StockDialogType.DELETE_DIALOG);
                    dialogFragments.show(manager , "deleteDialogFragment");
                }
                case EDIT_DIALOG -> {
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
                    dialogFragments.setEditBinding(formingEditView());
                    dialogFragments.show(manager , "editDialogFragment");
                }
                case ERROR_DIALOG -> {
                    var message = Optional.ofNullable(errorMessage);
                    dialogFragments.setDialogType(StockDialogType.ERROR_DIALOG);
                    message.ifPresent(s -> dialogFragments.setMessage(s));
                    dialogFragments.show(manager , "errorDialogFragment");
                }
                case MIGRATION_DIALOG -> {
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.MIGRATION_DIALOG);
                    dialogFragments.show(manager , "migrationDialogFragment");
                }
                case SELECT_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    var names = new ArrayList<String>();
                    currGameData.gameFiles.forEach(documentFile ->
                            names.add(documentFile.getName()));
                    dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
                    dialogFragments.setNames(names);
                    dialogFragments.show(manager , "selectDialogFragment");
                }
            }
        }
    }

    public DialogAddBinding setupAddView(DocumentFile rootDir) {
        addBinding = DialogAddBinding.inflate(LayoutInflater.from(getApplication()));

        var titleText = addBinding.ET0.getEditText();
        if (titleText != null) {
            titleText.setText(rootDir.getName());
        }
        addBinding.buttonSelectIcon.setOnClickListener(this::sendIntent);
        addBinding.addBT.setOnClickListener(v -> createAddIntent(rootDir));

        return addBinding;
    }

    private void createAddIntent(DocumentFile rootDir) {
        try {
            var newGameEntry = new Game();
            var editTextTitle = addBinding.ET0.getEditText();
            if (editTextTitle != null) {
                newGameEntry.title = editTextTitle.getText().toString().isEmpty()
                        ? rootDir.getName()
                        : editTextTitle.getText().toString();
            }
            var editTextAuthor = addBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                newGameEntry.author = editTextAuthor.getText().toString();
            }
            var editTextVersion = addBinding.ET2.getEditText();
            if (editTextVersion != null) {
                newGameEntry.version = editTextVersion.getText().toString();
            }
            if (tempImageFile != null) {
                newGameEntry.icon = tempImageFile.getUri().toString();
            }
            if (!addBinding.sizeDirSW.isChecked()) {
                newGameEntry.fileSize = DISABLE_CALCULATE_DIR;
            }
            localGame.createEntryInDB(newGameEntry);
            outputIntObserver.setValue(1);
            loadGameDataFromDB();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage() , ErrorType.EXCEPTION);
        }
    }

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding = DialogEditBinding.inflate(LayoutInflater.from(getStockActivity()));
        editBinding.setStockVM(this);

        getCurrGameData().ifPresent(gameData -> {
            var pathIcon = gameData.icon;
            if (pathIcon == null || pathIcon.isEmpty()) return;
            Picasso.get()
                    .load(pathIcon)
                    .fit()
                    .into(editBinding.imageView);
        });

        if (currGameData.getFileSize().equals(DISABLE_CALCULATE_DIR)) {
            editBinding.sizeDirSW.setChecked(false);
        }

        editBinding.buttonSelectPath.setOnClickListener(this::sendIntent);
        editBinding.buttonSelectMod.setOnClickListener(this::sendIntent);
        editBinding.buttonSelectIcon.setOnClickListener(this::sendIntent);
        editBinding.editBT.setOnClickListener(v -> createEditIntent());

        return editBinding;
    }

    public void createEditIntent() {
        try {
            var editTextTitle = editBinding.ET0.getEditText();
            if (editTextTitle != null) {
                currGameData.title = editTextTitle.getText().toString().isEmpty()
                        ? removeExtension(currGameData.title)
                        : editTextTitle.getText().toString();
            }

            var editTextAuthor = editBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                currGameData.author = editTextAuthor.getText().toString().isEmpty()
                        ? removeExtension(currGameData.author)
                        : editTextAuthor.getText().toString();
            }

            var editTextVersion = editBinding.ET2.getEditText();
            if (editTextVersion != null) {
                currGameData.version = editTextVersion.toString().isEmpty()
                        ? removeExtension(currGameData.version)
                        : editTextVersion.getText().toString();
            }

            if (tempImageFile != null) currGameData.icon = tempImageFile.getUri().toString();

            if (editBinding.sizeDirSW.isChecked() || currGameData.getFileSize().isEmpty()) {
                calculateSizeDir(currGameData);
            }

            if (tempPathFile != null) {
                copyFileToDir(getApplication() , tempPathFile , currGameData.gameDir);
            }

            if (tempModFile != null) {
                var modDir = findFileOrDirectory(
                        getApplication() ,
                        currGameData.gameDir ,
                        "mods"
                );
                copyFileToDir(getApplication() , tempModFile , modDir);
            }

            localGame.updateGameEntryInDB(currGameData)
                    .thenRun(this::loadGameDataFromDB);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage() , ErrorType.EXCEPTION);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = gameData.gameDir;

        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir) , executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = formatFileSize(aLong , getController().binaryPrefixes);
                    localGame.createDataIntoFolder(getApplication() , gameData , gameData.gameDir);
                });
    }

    public Optional<Intent> createPlayGameIntent() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (data.gameDir == null) return Optional.empty();
            var intent = new Intent(getApplication() , GameActivity.class);
            var gameDir = data.gameDir;

            var application = (QuestPlayerApplication) getApplication();
            application.setCurrentGameDir(gameDir);

            intent.putExtra("gameId" , data.id);
            intent.putExtra("gameTitle" , data.title);
            intent.putExtra("gameDirUri" , String.valueOf(gameDir.getUri()));

            return Optional.of(intent);
        } else {
            return Optional.empty();
        }
    }

    @SuppressLint("NonConstantResourceId")
    public void sendIntent(@NonNull View view) {
        switch (view.getId()) {
            case R.id.buttonSelectIcon ->
                    doOnShowFilePicker(CODE_PICK_IMAGE_FILE , new String[]{"image/png" , "image/jpeg"});
            case R.id.buttonSelectPath ->
                    doOnShowFilePicker(CODE_PICK_PATH_FILE , new String[]{"application/octet-stream"});
            case R.id.buttonSelectMod ->
                    doOnShowFilePicker(CODE_PICK_MOD_FILE , new String[]{"application/octet-stream"});
        }
    }
    // endregion Dialog

    public void startMigration() {
        var cache = getApplication().getExternalCacheDir();
        var listDirsFile = new File(cache , "tempListDir");

        if (!listDirsFile.exists()) return;

        try {
            var ref = new TypeReference<HashMap<String , String>>() {};
            var mapFiles = jsonToObject(listDirsFile , ref);
            var listFile = new ArrayList<DocumentFile>();
            for (var value : mapFiles.values()) {
                var uri = Uri.parse(value);
                var file = DocumentFileCompat.fromUri(getApplication() , uri);
                listFile.add(file);
            }
            endMigration(listFile);
        } catch (IOException e) {
            Log.e(TAG , "Error: ", e);
        }
    }

    private void endMigration(ArrayList<DocumentFile> listGamesDir) {
        CompletableFuture
                .supplyAsync(() -> localGame.extractDataFromList(getApplication() , listGamesDir), executor)
                .thenApply(gameDataList -> {
                    var listGame = new ArrayList<Game>();

                    gameDataList.forEach(gameData -> {
                        var emptyGameEntry = new Game();

                        emptyGameEntry.id = gameData.id;
                        emptyGameEntry.author = gameData.author;
                        emptyGameEntry.portedBy = gameData.portedBy;
                        emptyGameEntry.version = gameData.version;
                        emptyGameEntry.title = gameData.title;
                        emptyGameEntry.lang = gameData.lang;
                        emptyGameEntry.player = gameData.player;
                        emptyGameEntry.icon = gameData.icon;
                        emptyGameEntry.fileUrl = gameData.fileUrl;
                        emptyGameEntry.fileSize = DISABLE_CALCULATE_DIR;
                        emptyGameEntry.fileExt = gameData.fileExt;
                        emptyGameEntry.descUrl = gameData.descUrl;
                        emptyGameEntry.pubDate = gameData.pubDate;
                        emptyGameEntry.modDate = gameData.modDate;
                        emptyGameEntry.gameDirUri = gameData.gameDir.getUri();

                        var gameUriList = new ArrayList<Uri>();
                        gameData.gameFiles.forEach(documentFile ->
                                gameUriList.add(documentFile.getUri()));
                        emptyGameEntry.gameFilesUri = gameUriList;

                        listGame.add(emptyGameEntry);
                    });
                    return listGame;
                })
                .thenAcceptAsync(gameDao::insertAll, executor)
                .thenRun(this::loadGameDataFromDB);
    }

    // region Refresh
    public void createGameDataInDB(DocumentFile gameFile) {
        localGame.createGameEntryInDB(gameFile)
                .thenRun(this::loadGameDataFromDB)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: ", throwable);
                    return null;
                });
    }

    public void loadGameDataFromDB() {
        gamesMap.clear();

        localGame.getGameDataFromDB()
                .thenApply(dataBaseGameData -> {
                    dataBaseGameData.forEach(gameData -> {
                        var remoteGameData = gamesMap.get(gameData.id);
                        var size = Long.parseLong(gameData.fileSize);
                        if (remoteGameData != null) {
                            var aggregateGameData = new GameData(remoteGameData);
                            aggregateGameData.gameDir = gameData.gameDir;
                            aggregateGameData.gameFiles = gameData.gameFiles;
                            aggregateGameData.fileSize = formatFileSize(size, getController().binaryPrefixes);
                            gamesMap.put(gameData.id , aggregateGameData);
                        } else {
                            gameData.fileSize = formatFileSize(size, getController().binaryPrefixes);
                            gamesMap.put(gameData.id , gameData);
                        }
                    });
                    return gamesMap;
                })
                .thenAccept(gameDataHashMap -> {
                    var installedGames = new ArrayList<GameData>();
                    for (var data : gameDataHashMap.values()) {
                        if (!data.isFileInstalled()) continue;
                        if (!data.fileSize.equals(DISABLE_CALCULATE_DIR)) {
                            calculateSizeDir(data);
                        }
                        installedGames.add(data);
                    }
                    postValueGameDataList(installedGames);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: ", throwable);
                    doOnShowErrorDialog(throwable.getMessage() , ErrorType.EXCEPTION);
                    return null;
                });
    }

    private void dropPersistable(Uri folderUri) {
        try {
            var contentResolver = getApplication().getContentResolver();
            contentResolver.releasePersistableUriPermission(
                    folderUri ,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (SecurityException ignored) {}
    }

    public void removeEntryAndDirFromDB(String dirName) {
        CompletableFuture
                .supplyAsync(() -> gameDao.getByName(dirName) , executor)
                .thenApplyAsync(game -> {
                    dropPersistable(game.gameDirUri);
                    return game;
                })
                .thenApplyAsync(game -> {
                    ((QuestPlayerApplication) getApplication()).setCurrentGameDir(null);
                    return game;
                })
                .thenApply(game -> {
                    var uri = DocumentFileCompat.fromUri(getApplication() , game.gameDirUri);
                    if (uri == null) return game;
                    forceDelFile(getApplication() , uri);
                    return game;
                })
                .thenAcceptAsync(gameDao::delete , executor)
                .thenRun(this::loadGameDataFromDB)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    doOnShowErrorDialog(throwable.getMessage() , ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void removeEntryFromDB(String dirName) {
        CompletableFuture
                .supplyAsync(() -> gameDao.getByName(dirName) , executor)
                .thenApplyAsync(game -> {
                    dropPersistable(game.gameDirUri);
                    return game;
                })
                .thenApplyAsync(game -> {
                    ((QuestPlayerApplication) getApplication()).setCurrentGameDir(null);
                    return game;
                })
                .thenAcceptAsync(gameDao::delete , executor)
                .thenRun(this::loadGameDataFromDB)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    doOnShowErrorDialog(throwable.getMessage() , ErrorType.EXCEPTION);
                    return null;
                });
    }

    // endregion Refresh
}
