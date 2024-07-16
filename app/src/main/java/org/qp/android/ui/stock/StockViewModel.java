package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.forceDelFile;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.CreateMode;
import com.anggrayudi.storage.file.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.dto.stock.GameDataList;
import org.qp.android.dto.stock.RemoteGameData;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.EventEmitter;
import org.qp.android.model.repository.LocalGame;
import org.qp.android.model.repository.RemoteGame;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;
import org.qp.android.ui.settings.SettingsController;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockViewModel extends AndroidViewModel {

    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public static final String DISABLE_CALCULATE_DIR = "disable";
    private static final String INNER_GAME_DIR_NAME = "games-dir";

    public MutableLiveData<StockActivity> activityObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;

    // Containers
    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final MutableLiveData<List<GameData>> gameDataList = new MutableLiveData<>();
    private final MutableLiveData<List<RemoteGameData>> remoteDataList = new MutableLiveData<>();
    private ArrayList<DocumentFile> listGamesDir = new ArrayList<>();

    private final LocalGame localGame = new LocalGame();
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private GameData currGameData;
    private DialogEditBinding editBinding;
    private DialogAddBinding addBinding;
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    private final File listDirsFile;

    public EventEmitter emitter = new EventEmitter();

    private final FileCallback callback = new FileCallback() {
        @Override
        public void onConflict(@NonNull DocumentFile destinationFile,
                               @NonNull FileConflictAction action) {
            action.confirmResolution(ConflictResolution.REPLACE);
        }
    };

    // region Getter/Setter
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

    public void postValueRemoteDataList(List<RemoteGameData> remoteData) {
        remoteDataList.postValue(remoteData);
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
    private SettingsController getController() {
        return SettingsController.newInstance(getApplication());
    }

    @NonNull
    public ArrayList<GameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);
        if (gameData.size() < 2) return gameData;
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase(Locale.ROOT)));
        return gameData;
    }

    public LiveData<List<GameData>> getGameDataList() {
        return gameDataList;
    }

    public LiveData<List<RemoteGameData>> getRemoteDataList() {
        return remoteDataList;
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
            if (data.getFileSize() != null
                    && !data.getFileSize().equals(DISABLE_CALCULATE_DIR)) {
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

    public ArrayList<DocumentFile> getListGamesDir() {
        return listGamesDir;
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

    public void putGameDirToList(DocumentFile gameDir) {
        listGamesDir.add(gameDir);
    }

    public void doOnShowFilePicker(int requestCode , String[] mimeTypes) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowFilePicker(requestCode , mimeTypes));
    }

    public void doOnShowErrorDialog(String errorMessage , ErrorType errorType) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowErrorDialog(errorMessage , errorType));
    }

    public void doOnShowGameFragment(int position, int pageNum) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowGameFragment(position, pageNum));
    }

    public void doOnShowActionMode() {
        emitter.emitAndExecute(new StockFragmentNavigation.ShowActionMode());
    }

    public StockViewModel(@NonNull Application application) {
        super(application);

        var cache = getApplication().getExternalCacheDir();
        listDirsFile = new File(cache, "tempListDir");
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
            var nameDir = rootDir.getName();
            if (nameDir == null) {
                var secureRandom = new SecureRandom();
                nameDir = "game#" + secureRandom.nextInt();
            }

            var newGameData = new GameData();
            newGameData.id = nameDir.toLowerCase(Locale.ROOT);
            var editTextTitle = addBinding.ET0.getEditText();
            if (editTextTitle != null) {
                newGameData.title = editTextTitle.getText().toString().isEmpty()
                        ? nameDir
                        : editTextTitle.getText().toString();
            }
            var editTextAuthor = addBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                newGameData.author = editTextAuthor.getText().toString();
            }
            var editTextVersion = addBinding.ET2.getEditText();
            if (editTextVersion != null) {
                newGameData.version = editTextVersion.getText().toString();
            }
            if (tempImageFile != null) {
                newGameData.icon = tempImageFile.getUri().toString();
            }
            if (!addBinding.sizeDirSW.isChecked()) {
                newGameData.fileSize = DISABLE_CALCULATE_DIR;
            }

            localGame.createDataIntoFolder(getApplication(), newGameData, rootDir);
            outputIntObserver.setValue(1);
            refreshGamesDirs();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage() , ErrorType.EXCEPTION);
        }
    }

    @NonNull
    private DialogEditBinding formingEditView() {
        editBinding = DialogEditBinding.inflate(LayoutInflater.from(getApplication()));
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
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempPathFile, currGameData.gameDir, callback
                        ));
            }
            if (tempModFile != null) {
                var modDir = fromRelPath(getApplication(), "mods", currGameData.gameDir);
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempModFile, modDir, callback
                        ));
            }

            localGame.createDataIntoFolder(getApplication() , currGameData , currGameData.gameDir);
            refreshGamesDirs();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage() , ErrorType.EXCEPTION);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = gameData.gameDir;

        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir), executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = String.valueOf(aLong);
                    localGame.createDataIntoFolder(getApplication(), gameData, gameData.gameDir);
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

    // region Refresh
    public void refreshGamesDirs() {
        var rootExDir = ((QuestPlayerApplication) getApplication()).getCurrentGameDir();
        var rootInDir = getApplication().getExternalFilesDir(null);

        CompletableFuture
                .runAsync(() -> {
                    if (isWritableDir(getApplication(), rootExDir)) {
                        putGameDirToList(rootExDir);
                        refreshGameData();
                    } else {
                        doOnShowErrorDialog(null, ErrorType.FOLDER_ERROR);
                        var dirName = Optional.ofNullable(rootExDir.getName());
                        dirName.ifPresent(s -> removeDirFromListDirsFile(listDirsFile, s));
                    }
                }, executor);

        CompletableFuture
                .runAsync(() -> {
                    var gamesInDir = findOrCreateFolder(getApplication(), rootInDir, INNER_GAME_DIR_NAME);
                    if (isWritableDir(getApplication(), gamesInDir)) {
                        refreshRemoteData();
                    }
                }, executor);
    }

    public void refreshGameData() {
        gamesMap.clear();

        CompletableFuture
                .supplyAsync(() -> localGame.extractDataFromList(getApplication() , listGamesDir), executor)
                .thenAccept(innerGameData -> innerGameData.forEach(localGameData -> {
                    var remoteGameData = gamesMap.get(localGameData.id);
                    if (remoteGameData != null) {
                        var aggregateGameData = new GameData(remoteGameData);
                        aggregateGameData.gameDir = localGameData.gameDir;
                        aggregateGameData.gameFiles = localGameData.gameFiles;
                        gamesMap.put(localGameData.id , aggregateGameData);
                    } else {
                        gamesMap.put(localGameData.id , localGameData);
                    }
                }))
                .thenRunAsync(() -> {
                    var sortedGameData = getSortedGames();
                    var localGameData = new ArrayList<GameData>();
                    for (var data : sortedGameData) {
                        if (!data.isFileInstalled()) continue;
                        if (isNotEmpty(data.fileSize)) {
                            if (!data.fileSize.equals(DISABLE_CALCULATE_DIR)) {
                                var fileSize = Long.parseLong(data.fileSize);
                                var currPrefix = getController().binaryPrefixes;
                                data.fileSize = formatFileSize(fileSize, currPrefix);
                            }
                        } else {
                            calculateSizeDir(data);
                        }
                        localGameData.add(data);
                    }
                    postValueGameDataList(localGameData);
                } , executor);
    }

    public void refreshRemoteData() {
        var remoteGame = new RemoteGame();
        remoteGame.getRemoteGameData(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                var mapper = new XmlMapper();
                try (var body = response.body()){
                    if (body == null) return;
                    var string = body.string();
                    var value = mapper.readValue(string, GameDataList.class);
                    postValueRemoteDataList(value.game);
                } catch (IOException e) {
                    Log.e(TAG, "Error:", e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call,
                                  @NonNull Throwable throwable) {
                Log.e(TAG, "Error:", throwable);
            }
        });
    }

    // endregion Refresh

    // region Game list dir
    public void saveListDirsIntoFile(File listDirsFile) {
        var listFiles = new ArrayList<>(getListGamesDir());
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

    public void delEntryDirFromList(ArrayList<GameData> tempList, GameData  data, File listDirsFile) {
        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, data.gameDir.getName()) ,
                        (unused , unused2) -> null ,
                        executor
                )
                .thenRunAsync(() -> forceDelFile(getApplication(), data.gameDir) , executor)
                .thenRunAsync(() -> dropPersistable(data.gameDir.getUri()), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void delEntryFromList(ArrayList<GameData> tempList, GameData  data, File listDirsFile) {
        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, data.gameDir.getName()),
                        (unused , unused2) -> null,
                        executor
                )
                .thenRunAsync(() -> dropPersistable(data.gameDir.getUri()), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    private CompletableFuture<Void> removeDirFromListDirsFile(File listDirsFile , String folderName) {
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
                    doOnShowErrorDialog(throwable.getMessage() , ErrorType.EXCEPTION);
                    return null;
                });
    }

    // endregion Game list dir

    public void startFileDownload(GameData gameData) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var convUrl = new URL(gameData.fileUrl);

                        var cookie = CookieManager.getInstance().getCookie(gameData.fileUrl);
                        var con = (HttpURLConnection) convUrl.openConnection();
                        con.setRequestProperty("Cookie", cookie);
                        con.setRequestMethod("HEAD");
                        con.setInstanceFollowRedirects(false);
                        con.connect();

                        var content = con.getHeaderField("Content-Disposition");
                        var contentSplit = content.split("filename=");
                        return contentSplit[1].replace("filename=", "").replace("\"", "").trim();
                    } catch (IOException exception) {
                        Log.e(TAG, "Error:", exception);
                        return "";
                    }
                })
                .thenAccept(s -> {
                    if (s.isEmpty() || s.isBlank()) return;

                    var downManager = (DownloadManager) getApplication().getSystemService(Context.DOWNLOAD_SERVICE);

                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .mkdirs();

                    var downloadUri = Uri.parse(gameData.fileUrl);
                    var request = new DownloadManager.Request(downloadUri)
                            .setVisibleInDownloadsUi(true)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, s);
                    downManager.enqueue(request);
                });
    }
}
