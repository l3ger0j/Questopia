package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.FileUtil.isWritableDirectory;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;

import android.app.Application;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.helpers.repository.LocalGame;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;
import org.qp.android.ui.settings.SettingsController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class StockViewModel extends AndroidViewModel {

    private final String TAG = this.getClass().getSimpleName();

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public ObservableField<StockActivity> activityObservableField =
            new ObservableField<>();

    public ObservableBoolean isHideFAB = new ObservableBoolean();

    private final LocalGame localGame = new LocalGame();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private ArrayList<DocumentFile> listGamesDir;
    private DocumentFile tempImageFile, tempPathFile, tempModFile;

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

    public void setGameDataList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
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
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase(Locale.ROOT)));
        return gameData;
    }

    public LiveData<ArrayList<GameData>> getGameData() {
        return gameDataList;
    }

    public GameData getTempGameData() {
        return tempGameData;
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameAuthor() {
        if (tempGameData != null
                && tempGameData.author.length() > 0) {
            return getStockActivity()
                    .getString(R.string.author)
                    .replace("-AUTHOR-" , tempGameData.author);
        } else {
            return "";
        }
    }

    public String getGamePortBy() {
        if (tempGameData != null
                && tempGameData.portedBy.length() > 0) {
            return getStockActivity()
                    .getString(R.string.ported_by)
                    .replace("-PORTED_BY-" , tempGameData.portedBy);
        } else {
            return "";
        }
    }

    public String getGameVersion() {
        if (tempGameData != null
                && tempGameData.version.length() > 0) {
            return getStockActivity()
                    .getString(R.string.version)
                    .replace("-VERSION-" , tempGameData.version);
        } else {
            return "";
        }
    }

    public String getGameType() {
        if (tempGameData != null
                && tempGameData.fileExt.length() > 0) {
            if (tempGameData.fileExt.equals("aqsp")) {
                return getStockActivity()
                        .getString(R.string.fileType)
                        .replace("-TYPE-" , tempGameData.fileExt)
                        + " " + getStockActivity().getString(R.string.experimental);
            }
            return getStockActivity()
                    .getString(R.string.fileType)
                    .replace("-TYPE-" , tempGameData.fileExt);
        } else {
            return "";
        }
    }

    public String getGameSize() {
        if (tempGameData.getFileSize() != null) {
            return getStockActivity()
                    .getString(R.string.fileSize)
                    .replace("-SIZE-" , tempGameData.getFileSize());
        } else {
            return "";
        }
    }

    public String getGamePubData() {
        if (tempGameData != null &&
                tempGameData.pubDate.length() > 0) {
            return getStockActivity()
                    .getString(R.string.pub_data)
                    .replace("-PUB_DATA-" , tempGameData.pubDate);
        } else {
            return "";
        }
    }

    public String getGameModData() {
        if (tempGameData != null
                && tempGameData.modDate.length() > 0) {
            return getStockActivity()
                    .getString(R.string.mod_data)
                    .replace("-MOD_DATA-" , tempGameData.pubDate);
        } else {
            return "";
        }
    }

    public SettingsController getSettingsController() {
        return SettingsController.newInstance(getApplication());
    }

    public ArrayList<DocumentFile> getListGamesDir() {
        return listGamesDir;
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled() {
        return tempGameData.isFileInstalled() && isDirContainsGameFile(tempGameData.gameDir);
    }

    public boolean isHasRemoteUrl() {
        return tempGameData.isHasRemoteUrl();
    }

    public boolean isModsDirExist() {
        var modDir = tempGameData.gameDir.findFile("mods");
        return modDir != null;
    }
    // endregion Getter/Setter

    public void putGameDirToList(DocumentFile gameDir) {
        listGamesDir.add(gameDir);
    }

    public StockViewModel(@NonNull Application application) {
        super(application);
        gameDataList = new MutableLiveData<>();
        listGamesDir = new ArrayList<>();
    }

    // region Dialog
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    public void showDialogEdit() {
        dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
        dialogFragments.setEditBinding(formingEditView());
        getStockActivity()
                .showDialogFragment(dialogFragments , StockDialogType.EDIT_DIALOG);
        isHideFAB.set(true);
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
                calculateSizeDir(tempGameData);
            }
            if (tempPathFile != null) {
                copyFileToDir(getStockActivity() , tempPathFile , tempGameData.gameDir);
            }
            if (tempModFile != null) {
                var modDir = findFileOrDirectory(getStockActivity() , tempGameData.gameDir , "mods");
                copyFileToDir(getStockActivity() , tempModFile , modDir);
            }

            localGame.createDataIntoFolder(getApplication() , tempGameData , tempGameData.gameDir);
            refreshIntGamesDirectory();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            var message = getStockActivity().getString(R.string.error)+": "+ex;
            getStockActivity().showErrorDialog(message);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = gameData.gameDir;

        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir) , executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = formatFileSize(aLong , controller.binaryPrefixes);
                    localGame.createDataIntoFolder(getApplication() , gameData , gameData.gameDir);
                });
    }

    public void playGame() {
        var gameDir = tempGameData.gameDir;
        var gameFileCount = tempGameData.gameFiles.size();
        var intent = new Intent(getStockActivity() , GameActivity.class);

        var application = (QuestPlayerApplication) getApplication();
        application.setCurrentGameDir(gameDir);

        intent.putExtra("gameId" , tempGameData.id);
        intent.putExtra("gameTitle" , tempGameData.title);
        intent.putExtra("gameDirUri" , String.valueOf(gameDir.getUri()));

        switch (gameFileCount) {
            case 0 -> {
                var message = getStockActivity().getString(R.string.gameFolderEmpty);
                getStockActivity().showErrorDialog(message);
            }
            case 1 -> {
                var chosenGameFile = tempGameData.gameFiles.get(0);
                intent.putExtra("gameFileUri" ,  String.valueOf(chosenGameFile.getUri()));
                getStockActivity().startGameActivity(intent);
            }
            default -> {
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
                        .showDialogFragment(dialogFragments , StockDialogType.SELECT_DIALOG);
                outputIntObserver.observeForever(integer -> {
                    var chosenGameFile = tempGameData.gameFiles.get(integer);
                    intent.putExtra("gameFileUri" , String.valueOf(chosenGameFile.getUri()));
                    getStockActivity().startGameActivity(intent);
                });
            }
        }
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
            if (!isWritableDirectory(rootDir)) {
                var dirName = Optional.ofNullable(rootDir.getName());
                var message = getStockActivity().getString(R.string.gamesFolderError);
                getStockActivity().showErrorDialog(message);
                dirName.ifPresent(s -> getStockActivity().removeDirFromListDirsFile(s));
            } else {
                putGameDirToList(rootDir);
                refreshGameData();
            }
        }
    }

    public void refreshGameData() {
        gamesMap.clear();

        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        CompletableFuture
                .supplyAsync(() -> localGame.extractGameDataFromList(getStockActivity() , listGamesDir) , executor)
                .thenAccept(innerGameData -> {
                    for (var localGameData : innerGameData) {
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
                    setGameDataList(localGameData);
                } , executor);
    }
    // endregion Refresh
}
