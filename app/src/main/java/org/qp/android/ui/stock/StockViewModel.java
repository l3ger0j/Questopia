package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.FileUtil.isWritableDirectory;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
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
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.helpers.bus.EventEmitter;
import org.qp.android.helpers.repository.LocalGame;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class StockViewModel extends AndroidViewModel {

    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public MutableLiveData<StockActivity> activityObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;

    // Containers
    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final MutableLiveData<ArrayList<GameData>> gameDataList = new MutableLiveData<>();
    private ArrayList<DocumentFile> listGamesDir = new ArrayList<>();

    private final LocalGame localGame = new LocalGame();
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private GameData currGameData;
    private DialogEditBinding editBinding;
    private SettingsController controller;

    @Inject private GameDatabase gameDatabase;
    @Inject private GameDao gameDao;

    public EventEmitter emitter = new EventEmitter();


    // region Getter/Setter
    public void setController(SettingsController controller) {
        this.controller = controller;
    }

    public void setCurrGameData(GameData currGameData) {
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

    public void setListGamesDir(ArrayList<DocumentFile> gameDir) {
        listGamesDir = gameDir;
    }

    public void setValueGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.setValue(gameDataArrayList);
    }

    public void postValueGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
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

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);
        if (gameData.size() < 2) return gameData;
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase(Locale.ROOT)));
        return gameData;
    }

    public LiveData<ArrayList<GameData>> getGameDataList() {
        return gameDataList;
    }

    public Optional<GameData> getCurrGameData() {
        return Optional.ofNullable(currGameData);
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameTitle() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.title.isEmpty()) {
                return data.title;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    public String getGameAuthor() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.author.isEmpty()) {
                return getStockActivity()
                        .getString(R.string.author)
                        .replace("-AUTHOR-" , data.author);
            }
        }
        return "";
    }

    public String getGameIcon() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.icon.isEmpty()) {
                return data.icon;
            }
        }
        return "";
    }

    public String getGamePortBy() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.portedBy.isEmpty()) {
                return getStockActivity()
                        .getString(R.string.ported_by)
                        .replace("-PORTED_BY-" , data.portedBy);
            }
        }
        return "";
    }

    public String getGameVersion() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.version.isEmpty()) {
                return getStockActivity()
                        .getString(R.string.version)
                        .replace("-VERSION-" , data.version);
            }
        }
        return "";
    }

    public String getGameType() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.fileExt.isEmpty()) {
                if (data.fileExt.equals("aqsp")) {
                    return getStockActivity()
                            .getString(R.string.fileType)
                            .replace("-TYPE-" , data.fileExt)
                            + " " + getStockActivity().getString(R.string.experimental);
                } else {
                    return getStockActivity()
                            .getString(R.string.fileType)
                            .replace("-TYPE-" , data.fileExt);
                }
            }
        }
        return "";
    }

    public String getGameSize() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (data.getFileSize() != null) {
                return getStockActivity()
                        .getString(R.string.fileSize)
                        .replace("-SIZE-" , data.getFileSize());
            }
        }
        return "";
    }

    public String getGamePubData() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.pubDate.isEmpty()) {
                return getStockActivity()
                        .getString(R.string.pub_data)
                        .replace("-PUB_DATA-" , data.pubDate);
            }
        }
        return "";
    }

    public String getGameModData() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            if (!data.modDate.isEmpty()) {
                return getStockActivity()
                        .getString(R.string.mod_data)
                        .replace("-MOD_DATA-" , data.pubDate);
            }
        }
        return "";
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    public ArrayList<DocumentFile> getListGamesDir() {
        return listGamesDir;
    }

    public int getCountGameFiles() {
        return currGameData.gameFiles.size();
    }

    public DocumentFile getGameFile(int index) {
        return currGameData.gameFiles.get(index);
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

    public void putGameDirToList(DocumentFile gameDir) {
        listGamesDir.add(gameDir);
    }

    public void doOnShowGameFragment(int position) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowGameFragment(position));
    }

    public void doOnShowActionMode() {
        emitter.emitAndExecute(new StockFragmentNavigation.ShowActionMode());
    }

    public StockViewModel(@NonNull Application application) {
        super(application);
    }

    // region Dialog
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    public void showDialogFragment(FragmentManager manager ,
                                   StockDialogType dialogType ,
                                   String errorMessage) {
        var fragment = manager.findFragmentByTag(dialogFragments.getTag());
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            switch (dialogType) {
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

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding = DialogEditBinding.inflate(LayoutInflater.from(getApplication()));
        editBinding.setStockVM(this);

        if (!currGameData.icon.isEmpty()) {
            Picasso.get()
                    .load(currGameData.icon)
                    .fit()
                    .into(editBinding.imageView);
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
            if (currGameData.fileSize == null || currGameData.fileSize.isEmpty()) {
                calculateSizeDir(currGameData);
            }
            if (tempPathFile != null) {
                copyFileToDir(getApplication() , tempPathFile , currGameData.gameDir);
            }
            if (tempModFile != null) {
                var modDir = findFileOrDirectory(getStockActivity() , currGameData.gameDir , "mods");
                copyFileToDir(getApplication() , tempModFile , modDir);
            }

            localGame.createDataIntoFolder(getApplication() , currGameData , currGameData.gameDir);
            refreshIntGamesDirectory();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            var message = getStockActivity().getString(R.string.error) + ": " + ex;
            getStockActivity().showErrorDialog(message);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = gameData.gameDir;

        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir) , executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = formatFileSize(aLong , controller.binaryPrefixes);
                    localGame.createDataIntoFolder(getApplication() , gameData , gameData.gameDir);
                });
    }

    public Intent createPlayGameIntent() {
        var gameDir = currGameData.gameDir;
        var intent = new Intent(getApplication() , GameActivity.class);

        var application = (QuestPlayerApplication) getApplication();
        application.setCurrentGameDir(gameDir);

        intent.putExtra("gameId" , currGameData.id);
        intent.putExtra("gameTitle" , currGameData.title);
        intent.putExtra("gameDirUri" , String.valueOf(gameDir.getUri()));

        return intent;
    }

    public void sendIntent(@NonNull View view) {
        int id = view.getId();
        if (id == R.id.buttonSelectIcon) {
            getStockActivity()
                    .showFilePickerActivity(CODE_PICK_IMAGE_FILE , new String[]{"image/png" , "image/jpeg"});
        } else if (id == R.id.buttonSelectPath) {
            getStockActivity()
                    .showFilePickerActivity(CODE_PICK_PATH_FILE , new String[]{"application/octet-stream"});
        } else if (id == R.id.buttonSelectMod) {
            getStockActivity()
                    .showFilePickerActivity(CODE_PICK_MOD_FILE , new String[]{"application/octet-stream"});
        }
    }
    // endregion Dialog

    // region Refresh
    public void refreshIntGamesDirectory() {
        var rootDir = ((QuestPlayerApplication) getApplication()).getCurrentGameDir();
        if (rootDir != null) {
            CompletableFuture
                    .runAsync(() -> {
                        if (!isWritableDirectory(rootDir)) {
                            var dirName = Optional.ofNullable(rootDir.getName());
                            var message = getStockActivity().getString(R.string.gamesFolderError);
                            getStockActivity().showErrorDialog(message);
                            var saveDir = getStockActivity().getListDirsFile();
                            dirName.ifPresent(s -> removeDirFromListDirsFile(saveDir , s));
                        } else {
                            putGameDirToList(rootDir);
                            refreshGameData();
                        }
                    } , executor);
        }
    }

    public void refreshGameData() {
        gamesMap.clear();

        CompletableFuture
                .supplyAsync(() -> localGame.extractGameDataFromList(getStockActivity() , listGamesDir) , executor)
                .thenAccept(innerGameData -> {
                    for (var localGameData : innerGameData) {
                        var remoteGameData = gamesMap.get(localGameData.id);
                        if (remoteGameData != null) {
                            var aggregateGameData = new GameData(remoteGameData);
                            aggregateGameData.gameDir = localGameData.gameDir;
                            aggregateGameData.gameFiles = localGameData.gameFiles;
                            gamesMap.put(localGameData.id , aggregateGameData);
                        } else {
                            gamesMap.put(localGameData.id , localGameData);
                        }
                    }
                })
                .thenRunAsync(() -> {
                    var gameData = getSortedGames();
                    var localGameData = new ArrayList<GameData>();
                    for (var data : gameData) {
                        if (data.isFileInstalled()) {
                            if (data.fileSize == null || data.fileSize.isEmpty()) {
                                calculateSizeDir(data);
                            }
                            localGameData.add(data);
                        }
                    }
                    gameDataList.postValue(localGameData);
                } , executor);
    }
    // endregion Refresh

    // region Game list dir
    public void saveListDirsIntoFile(File listDirsFile) {
        var listFiles = getListGamesDir();
        var mapFiles = new HashMap<String, String>();

        CompletableFuture
                .runAsync(() -> {
                    for (var file : listFiles) {
                        if (file.getName() == null) continue;
                        var packedUri = String.valueOf(file.getUri());
                        mapFiles.put(file.getName() , packedUri);
                    }
                } , executor)
                .thenRunAsync(() -> {
                    try {
                        objectToJson(listDirsFile , mapFiles);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                } , executor)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> removeDirFromListDirsFile(File listDirsFile , String folderName) {
        var ref = new TypeReference<HashMap<String, String>>() {};

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return jsonToObject(listDirsFile , ref);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                } , executor)
                .thenAcceptAsync(mapFiles -> {
                    if (!mapFiles.isEmpty()) {
                        mapFiles
                                .entrySet()
                                .removeIf(stringStringEntry -> stringStringEntry.getKey().equalsIgnoreCase(folderName));
                        try {
                            objectToJson(listDirsFile , mapFiles);
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }
                } , executor)
                .thenRunAsync(() -> {
                    var optNewGameList = Optional.ofNullable(getListGamesDir());
                    optNewGameList.ifPresent(newGameList -> {
                        newGameList.removeIf(documentFile ->
                                documentFile.getName().equalsIgnoreCase(folderName));
                        setListGamesDir(newGameList);
                    });
                    ((QuestPlayerApplication) getApplication()).setCurrentGameDir(null);
                })
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    return null;
                });
    }

    // endregion Game list dir
}
