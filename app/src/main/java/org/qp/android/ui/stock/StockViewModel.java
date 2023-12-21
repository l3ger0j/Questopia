package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
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
import org.qp.android.dto.stock.InnerGameData;
import org.qp.android.helpers.repository.LocalGame;
import org.qp.android.model.workers.WorkerBuilder;
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
    private final HashMap<String, InnerGameData> gamesMap = new HashMap<>();
    private ArrayList<DocumentFile> listGamesDir;
    private DocumentFile tempImageFile, tempPathFile, tempModFile;

    private InnerGameData tempInnerGameData;
    private DialogEditBinding editBinding;
    private SettingsController controller;

    private final MutableLiveData<ArrayList<InnerGameData>> gameDataList;

    public MutableLiveData<Integer> outputIntObserver = new MutableLiveData<>();

    // region Getter/Setter
    public void setController(SettingsController controller) {
        this.controller = controller;
    }

    public void setTempGameData(InnerGameData tempInnerGameData) {
        this.tempInnerGameData = tempInnerGameData;
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

    public void setGameDataList(ArrayList<InnerGameData> innerGameDataArrayList) {
        gameDataList.postValue(innerGameDataArrayList);
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
    public ArrayList<InnerGameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);
        if (gameData.size() < 2) return gameData;
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase(Locale.ROOT)));
        return gameData;
    }

    public LiveData<ArrayList<InnerGameData>> getGameData() {
        return gameDataList;
    }

    public InnerGameData getTempGameData() {
        return tempInnerGameData;
    }

    public HashMap<String, InnerGameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameAuthor() {
        if (tempInnerGameData != null
                && tempInnerGameData.author.length() > 0) {
            return getStockActivity()
                    .getString(R.string.author)
                    .replace("-AUTHOR-" , tempInnerGameData.author);
        } else {
            return "";
        }
    }

    public String getGamePortBy() {
        if (tempInnerGameData != null
                && tempInnerGameData.portedBy.length() > 0) {
            return getStockActivity()
                    .getString(R.string.ported_by)
                    .replace("-PORTED_BY-" , tempInnerGameData.portedBy);
        } else {
            return "";
        }
    }

    public String getGameVersion() {
        if (tempInnerGameData != null
                && tempInnerGameData.version.length() > 0) {
            return getStockActivity()
                    .getString(R.string.version)
                    .replace("-VERSION-" , tempInnerGameData.version);
        } else {
            return "";
        }
    }

    public String getGameType() {
        if (tempInnerGameData != null
                && tempInnerGameData.fileExt.length() > 0) {
            if (tempInnerGameData.fileExt.equals("aqsp")) {
                return getStockActivity()
                        .getString(R.string.fileType)
                        .replace("-TYPE-" , tempInnerGameData.fileExt)
                        + " " + getStockActivity().getString(R.string.experimental);
            }
            return getStockActivity()
                    .getString(R.string.fileType)
                    .replace("-TYPE-" , tempInnerGameData.fileExt);
        } else {
            return "";
        }
    }

    public String getGameSize() {
        if (tempInnerGameData.getFileSize() != null) {
            return getStockActivity()
                    .getString(R.string.fileSize)
                    .replace("-SIZE-" , tempInnerGameData.getFileSize());
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
        return tempInnerGameData.isInstalled() && doesDirectoryContainGameFiles(tempInnerGameData.gameDir);
    }

    public boolean isHasRemoteUrl() {
        return tempInnerGameData.hasRemoteUrl();
    }

    public boolean isModsDirExist() {
        var modDir = tempInnerGameData.gameDir.findFile("mods");
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

        if (!tempInnerGameData.icon.isEmpty()) {
            Picasso.get()
                    .load(tempInnerGameData.icon)
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
                tempInnerGameData.title = editTextTitle.getText().toString().isEmpty()
                        ? removeExtension(tempInnerGameData.title)
                        : editTextTitle.getText().toString();
            }
            var editTextAuthor = editBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                tempInnerGameData.author = editTextAuthor.getText().toString().isEmpty()
                        ? removeExtension(tempInnerGameData.author)
                        : editTextAuthor.getText().toString();
            }
            var editTextVersion = editBinding.ET2.getEditText();
            if (editTextVersion != null) {
                tempInnerGameData.version = editTextVersion.toString().isEmpty()
                        ? removeExtension(tempInnerGameData.version)
                        : editTextVersion.getText().toString();
            }
            if (tempImageFile != null) tempInnerGameData.icon = tempImageFile.getUri().toString();
            if (tempInnerGameData.fileSize == null || tempInnerGameData.fileSize.isEmpty()) {
                calculateSizeDir(tempInnerGameData.gameDir).observeForever(aLong -> {
                    if (aLong != null) {
                        tempInnerGameData.fileSize = formatFileSize(aLong , controller.binaryPrefixes);
                    }
                });
            }
            localGame.createDataFileIntoFolder(getApplication() , tempInnerGameData , tempInnerGameData.gameDir);
            if (tempPathFile != null) {
                copyFileToDir(getStockActivity() , tempPathFile , tempInnerGameData.gameDir);
            }
            if (tempModFile != null) {
                var modDir = tempInnerGameData.gameDir.findFile("mods");
                copyFileToDir(getStockActivity() , tempModFile , modDir);
            }
            refreshIntGamesDirectory();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            var message = getStockActivity().getString(R.string.error)+": "+ex;
            getStockActivity().showErrorDialog(message);
        }
    }

    private LiveData<Long> calculateSizeDir(DocumentFile srcDir) {
        var installer = new WorkerBuilder(getStockActivity());
        return installer.calculateDirSize(srcDir);
    }

    public void playGame() {
        var gameDir = tempInnerGameData.gameDir;
        var gameFileCount = tempInnerGameData.gameFiles.size();
        var intent = new Intent(getStockActivity() , GameActivity.class);

        var application = (QuestPlayerApplication) getApplication();
        application.setCurrentGameDir(gameDir);

        intent.putExtra("gameId" , tempInnerGameData.id);
        intent.putExtra("gameTitle" , tempInnerGameData.title);
        intent.putExtra("gameDirUri" , String.valueOf(gameDir.getUri()));

        switch (gameFileCount) {
            case 0 -> {
                var message = getStockActivity().getString(R.string.gameFolderEmpty);
                getStockActivity().showErrorDialog(message);
            }
            case 1 -> {
                var chosenGameFile = tempInnerGameData.gameFiles.get(0);
                intent.putExtra("gameFileUri" ,  String.valueOf(chosenGameFile.getUri()));
                getStockActivity().startGameActivity(intent);
            }
            default -> {
                if (outputIntObserver.hasObservers()) {
                    outputIntObserver = new MutableLiveData<>();
                }
                var names = new ArrayList<String>();
                for (var file : tempInnerGameData.gameFiles) {
                    names.add(file.getName());
                }
                var dialogFragments = new StockDialogFrags();
                dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
                dialogFragments.setNames(names);
                getStockActivity()
                        .showDialogFragment(dialogFragments , StockDialogType.SELECT_DIALOG);
                outputIntObserver.observeForever(integer -> {
                    var chosenGameFile = tempInnerGameData.gameFiles.get(integer);
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

        CompletableFuture
                .supplyAsync(() -> localGame.extractGameDataFromList(getStockActivity() , listGamesDir) ,
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
                .thenApply(innerGameData -> {
                    for (var localGameData : innerGameData) {
                        var remoteGameData = gamesMap.get(localGameData.id);
                        if (remoteGameData != null) {
                            var aggregateGameData = new InnerGameData(remoteGameData);
                            aggregateGameData.gameDir = localGameData.gameDir;
                            aggregateGameData.gameFiles = localGameData.gameFiles;
                            gamesMap.put(localGameData.id, aggregateGameData);
                        } else {
                            gamesMap.put(localGameData.id, localGameData);
                        }
                    }
                    return null;
                })
                .thenRunAsync(() -> {
                    var gameData = getSortedGames();
                    var localGameData = new ArrayList<InnerGameData>();
                    for (var data : gameData) {
                        if (data.isInstalled()) {
                            localGameData.add(data);
                        }
                    }
                    setGameDataList(localGameData);
                } , Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }
    // endregion Refresh
}
