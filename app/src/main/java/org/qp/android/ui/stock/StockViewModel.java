package org.qp.android.ui.stock;

import static org.qp.android.QuestPlayerApplication.UNPACK_GAME_CHANNEL_ID;
import static org.qp.android.QuestPlayerApplication.UNPACK_GAME_NOTIFICATION_ID;
import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.forceDelFile;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationManager;
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
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.qp.android.QuestPlayerApplication;
import org.qp.android.R;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.dto.stock.RemoteGameData;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.EventEmitter;
import org.qp.android.model.archive.ArchiveUnpack;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.model.repository.LocalGame;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockViewModel extends AndroidViewModel {

    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public static final String DISABLE_CALCULATE_DIR = "disable";
    private static final String INNER_GAME_DIR_NAME = "games-dir";
    public static final String EXT_GAME_LIST_NAME = "extGameDirs";

    public MutableLiveData<StockActivity> activityObserver = new MutableLiveData<>();
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;

    public final MutableLiveData<Integer> currPageNumber = new MutableLiveData<>();

    public List<DocumentFile> extGamesListDir = new ArrayList<>();

    // Containers
    private final HashMap<String, GameData> gamesMap = new HashMap<>();
    private final MutableLiveData<List<GameData>> gameDataList = new MutableLiveData<>();

    private final LocalGame localGame = new LocalGame(getApplication());

    private final DownloadManager downloadManager = getApplication().getSystemService(DownloadManager.class);
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private GameData currGameData;
    private DialogEditBinding editBinding;
    private DialogAddBinding addBinding;
    private StockDialogFrags dialogFragments = new StockDialogFrags();

    private final File listDirsFile;
    private final File rootInDir;

    public EventEmitter emitter = new EventEmitter();

    private long downloadId = 0L;

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

    public void setValueGameDataList(List<GameData> gameDataArrayList) {
        gameDataList.setValue(gameDataArrayList);
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
    public List<GameData> getSortedGames() {
        var unsortedGameData = gamesMap.values();
        var gameData = new ArrayList<>(unsortedGameData);
        if (gameData.size() < 2) return gameData;
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase(Locale.ROOT)));
        gameData.sort(Comparator.comparing(game -> game.listId.toLowerCase(Locale.ROOT)));
        return gameData;
    }

    public LiveData<List<GameData>> getGameDataList() {
        return gameDataList;
    }

    public Optional<GameData> getCurrGameData() {
        return Optional.ofNullable(currGameData);
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameTitle() {
        var data = currGameData;
        if (data == null) return "";

        var title = data.title;
        if (title == null || title.isEmpty() || title.isBlank()) return "";

        return data.title;
    }

    public String getGameAuthor() {
        var data = currGameData;
        if (data == null) return "";

        var author = data.author;
        if (author == null || author.isEmpty() || author.isBlank()) return "";

        var authorString = getStockActivity().getString(R.string.author);
        return authorString.replace("-AUTHOR-", data.author);
    }

    public String getGameIcon() {
        var data = currGameData;
        if (data == null) return "";

        var icon = data.icon;
        if (icon == null || icon.isEmpty() || icon.isBlank()) return "";

        return icon;
    }

    public String getGamePortBy() {
        var data = currGameData;
        if (data == null) return "";

        var portedBy = data.portedBy;
        if (portedBy == null || portedBy.isEmpty() || portedBy.isBlank()) return "";

        var portedByString = getStockActivity().getString(R.string.ported_by);
        return portedByString.replace("-PORTED_BY-", data.portedBy);
    }

    public String getGameVersion() {
        var data = currGameData;
        if (data == null) return "";

        var version = data.version;
        if (version == null || version.isEmpty() || version.isBlank()) return "";

        var versionString = getStockActivity().getString(R.string.version);
        return versionString.replace("-VERSION-", data.version);
    }

    public String getGameType() {
        var data = currGameData;
        if (data == null) return "";

        var fileExt = data.fileExt;
        if (fileExt == null || fileExt.isEmpty() || fileExt.isBlank()) return "";

        var fileTypeSting = getStockActivity().getString(R.string.fileType);
        if (fileExt.equals("aqsp")) {
            var experimentalString = getStockActivity().getString(R.string.experimental);
            return fileTypeSting.replace("-TYPE-", data.fileExt) + " " + experimentalString;
        } else {
            return fileTypeSting.replace("-TYPE-", data.fileExt);
        }
    }

    public String getGameSize() {
        var data = currGameData;
        if (data == null) return "";

        var fileSize = data.fileSize;
        if (fileSize == null || fileSize.isEmpty() || fileSize.isBlank()) return "";
        if (fileSize.equals(DISABLE_CALCULATE_DIR)) return "";

        var fileSizeString = getStockActivity().getString(R.string.fileSize);
        return fileSizeString.replace("-SIZE-", fileSize);
    }

    public String getGamePubData() {
        var data = currGameData;
        if (data == null) return "";

        var pubDate = data.pubDate;
        if (pubDate == null || pubDate.isEmpty() || pubDate.isBlank()) return "";

        var pubDataString = getStockActivity().getString(R.string.pub_data);
        return pubDataString.replace("-PUB_DATA-", pubDate);
    }

    public String getGameModData() {
        var data = currGameData;
        if (data == null) return "";

        var modDate = data.modDate;
        if (modDate == null || modDate.isEmpty() || modDate.isBlank()) return "";

        var modDataString = getStockActivity().getString(R.string.mod_data);
        return modDataString.replace("-MOD_DATA-", modDate);
    }

    public File getListDirsFile() {
        return listDirsFile;
    }

    public int getCountGameFiles() {
        var gameData = currGameData;
        if (gameData == null) return 0;
        var gameFiles = gameData.getGameFiles(getApplication());
        return gameFiles.size();
    }

    @Nullable
    public DocumentFile getGameFile(int index) {
        var gameData = currGameData;
        if (gameData == null) return null;
        var gameFiles = gameData.getGameFiles(getApplication());
        return gameFiles.get(index);
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled() {
        var gameData = currGameData;
        if (gameData == null) return false;
        var gameDir = gameData.getGameDir(getApplication());
        return gameData.isFileInstalled() && isDirContainsGameFile(gameDir);
    }

    public boolean isHasRemoteUrl() {
        var gameData = currGameData;
        if (gameData == null) return false;
        return currGameData.isHasRemoteUrl();
    }

    public boolean isModsDirExist() {
        var gameData = currGameData;
        if (gameData == null) return false;
        var gameDir = gameData.getGameDir(getApplication());
        var modDir = fromRelPath(getApplication(), "mods", gameDir);
        return modDir != null;
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

    public StockViewModel(@NonNull Application application) {
        super(application);

        var cache = getApplication().getExternalCacheDir();
        listDirsFile = findOrCreateFile(getApplication(), cache, EXT_GAME_LIST_NAME , MimeType.TEXT);

        var rootInDir = getApplication().getExternalFilesDir(null);
        this.rootInDir = findOrCreateFolder(getApplication(), rootInDir, INNER_GAME_DIR_NAME);
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
                    var files = currGameData.getGameFiles(getApplication());
                    files.forEach(file -> names.add(file.getName()));
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

            localGame.createDataIntoFolder(newGameData, rootDir);
            outputIntObserver.setValue(1);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
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

        if (getGameSize().equals(DISABLE_CALCULATE_DIR)) {
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
            if (editBinding.sizeDirSW.isChecked() || getGameSize().isEmpty()) {
                calculateSizeDir(currGameData);
            }
            var gameDir = currGameData.getGameDir(getApplication());
            if (tempPathFile != null) {
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempPathFile, gameDir, callback
                        ));
            }
            if (tempModFile != null) {
                var modDir = fromRelPath(getApplication(), "mods", gameDir);
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempModFile, modDir, callback
                        ));
            }

            localGame.createDataIntoFolder(currGameData, gameDir);
            refreshGamesDirs();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage() , ErrorType.EXCEPTION);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = gameData.getGameDir(getApplication());

        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir), executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = String.valueOf(aLong);
                    localGame.createDataIntoFolder(gameData, gameDir);
                });
    }

    public Optional<Intent> createPlayGameIntent() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            var gameDir = data.getGameDir(getApplication());
            if (gameDir == null) return Optional.empty();
            var intent = new Intent(getApplication() , GameActivity.class);

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

        if (isWritableDir(getApplication(), rootExDir)) {
            extGamesListDir.add(rootExDir);
            refreshGameData();
        } else {
            if (rootExDir == null) return;
            doOnShowErrorDialog(null, ErrorType.FOLDER_ERROR);
            var dirName = rootExDir.getName();
            if (dirName == null) return;
            removeDirFromListDirsFile(listDirsFile, dirName);
        }
    }

    private CompletableFuture<List<GameData>> fetchInternalData() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return localGame.extractDataFromFolder(rootInDir);
                    } catch (IOException e) {
                        Log.d(TAG, "error: ", e);
                        throw new CompletionException(e);
                    }
                }, executor)
                .thenApplyAsync(intDataList -> {
                    var newList = new ArrayList<>(intDataList);
                    newList.forEach(item -> item.listId = String.valueOf(0));
                    return newList;
                }, executor);
    }

    private CompletableFuture<List<GameData>> fetchExternalData() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var copy = new ArrayList<>(extGamesListDir);
                        return localGame.extractDataFromList(copy);
                    } catch (IOException e) {
                        Log.d(TAG, "error: ", e);
                        throw new CompletionException(e);
                    }
                }, executor)
                .thenApplyAsync(extDataList -> {
                    var newList = new ArrayList<>(extDataList);
                    newList.forEach(item -> item.listId = String.valueOf(1));
                    return newList;
                }, executor);
    }

    private CompletableFuture<List<RemoteGameData>> fetchRemoteData() {
        return CompletableFuture
                .supplyAsync(() -> {
                    var cache = getApplication().getExternalCacheDir();
                    if (cache == null) return Collections.emptyList();
                    var listFiles = cache.listFiles();
                    if (listFiles == null) return Collections.emptyList();

                    for (var file : listFiles) {
                        if (file.getName().isEmpty()) continue;
                        if (!file.getName().equalsIgnoreCase(EXT_GAME_LIST_NAME)) {
                            var mapper = new XmlMapper();
                            try {
                                var ref = new TypeReference<ArrayList<RemoteGameData>>(){};
                                return mapper.readValue(file, ref);
                            } catch (IOException e) {
                                Log.d(TAG, "error: ", e);
                                throw new CompletionException(e);
                            }
                        }
                    }

                    return Collections.emptyList();
                }, executor);
    }

    public void refreshGameData() {
        gamesMap.clear();

        var pageNumber = currPageNumber.getValue();
        if (pageNumber == null) return;

        if (pageNumber == 0) {
            setValueGameDataList(Collections.emptyList());

            doIsHideFAB.setValue(false);

            syncFromDisk();
        }

        if (pageNumber == 1) {
            setValueGameDataList(Collections.emptyList());

            doIsHideFAB.setValue(true);

            syncRemote();
        }

    }

    private void syncFromDisk() {
        fetchInternalData()
                .thenCombineAsync(fetchExternalData(), (intDataList , extDataList) -> {
                    var unionList = new ArrayList<>(intDataList);
                    unionList.addAll(extDataList);
                    return unionList;
                }, executor)
                .thenAccept(externalGameData -> externalGameData.forEach(localGameData -> {
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
                        if (!isNotEmpty(data.fileSize)) {
                            calculateSizeDir(data);
                        } else {
                            if (!data.fileSize.equals(DISABLE_CALCULATE_DIR)) {
                                try {
                                    var fileSize = Long.parseLong(data.fileSize);
                                    var currPrefix = getController().binaryPrefixes;
                                    data.fileSize = formatFileSize(fileSize , currPrefix);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        localGameData.add(data);
                    }
                    gameDataList.postValue(localGameData);
                }, executor)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error: ", throwable);
                    return null;
                });
    }

    private void syncRemote() {
        fetchInternalData()
                .thenCombineAsync(fetchRemoteData(), (intDataList, remDataList) -> {
                    if (intDataList.isEmpty()) return remDataList;

                    var unionList = new ArrayList<RemoteGameData>();
                    intDataList.forEach(intData -> remDataList.forEach(remData -> {
                        if (!Objects.equals(intData.title, remData.title)) {
                            unionList.add(remData);
                        }
                    }));

                    return unionList;
                }, executor)
                .thenAcceptAsync(remDataList -> remDataList.forEach(remGameData ->
                        gamesMap.put(remGameData.id, new GameData(remGameData))
                ), executor)
                .thenRunAsync(() -> {
                    var sortedGameData = getSortedGames();
//                    var localGameData = new ArrayList<GameData>();
//                    for (var data : sortedGameData) {
//                        if (isNotEmpty(data.fileSize)) {
//                            if (!data.fileSize.equals(DISABLE_CALCULATE_DIR)) {
//                                var fileSize = Long.parseLong(data.fileSize);
//                                var currPrefix = getController().binaryPrefixes;
//                                data.fileSize = formatFileSize(fileSize , currPrefix);
//                            }
//                        } else {
//                            calculateSizeDir(data);
//                        }
//                        localGameData.add(data);
//                    }
                    gameDataList.postValue(sortedGameData);
                }, executor)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error: ", throwable);
                    return null;
                });
    }

    // endregion Refresh

    // region Game list dir
    public void saveListDirsIntoFile(File listDirsFile) {
        var listFiles = new ArrayList<>(extGamesListDir);
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

    public void delEntryDirFromList(List<GameData> tempList, GameData  data, File listDirsFile) {
        var gameDir = data.getGameDir(getApplication());

        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, gameDir.getName()) ,
                        (unused , unused2) -> null ,
                        executor
                )
                .thenRunAsync(() -> forceDelFile(getApplication(), gameDir), executor)
                .thenRunAsync(() -> dropPersistable(data.gameDir), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void delEntryFromList(List<GameData> tempList, GameData  data, File listDirsFile) {
        var gameDir = data.getGameDir(getApplication());

        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, gameDir.getName()),
                        (unused , unused2) -> null,
                        executor
                )
                .thenRunAsync(() -> dropPersistable(data.gameDir), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    private CompletableFuture<Void> removeDirFromListDirsFile(File listDirsFile, String folderName) {
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
                    var newList = extGamesListDir;
                    if (newList == null) return;

                    newList.removeIf(file -> {
                        var nameDir = file.getName();
                        return nameDir.equalsIgnoreCase(folderName);
                    });
                    extGamesListDir = newList;

                    ((QuestPlayerApplication) getApplication()).setCurrentGameDir(null);
                    getStockActivity().runOnUiThread(this::refreshGameData);
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

                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .mkdirs();

                    var downloadUri = Uri.parse(gameData.fileUrl);
                    var request = new DownloadManager.Request(downloadUri)
                            .setVisibleInDownloadsUi(true)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalFilesDir(getApplication(), Environment.DIRECTORY_DOWNLOADS, s);
                    downloadId = downloadManager.enqueue(request);
                })
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.getMessage() , ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void postProcessingDownload() {
        if (downloadId == 0) return;
        var query = new DownloadManager.Query()
                .setFilterById(downloadId);
        try (var c = downloadManager.query(query)) {
            if (c.moveToFirst()) {
                var colStatusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(colStatusIndex)) {
                    var colUriIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    if (colUriIndex == -1) return;
                    var path = c.getString(colUriIndex).replace("file:///", "");
                    var file = DocumentFileCompat.fromUri(getApplication() , Uri.parse(c.getString(colUriIndex)));
                    if (file == null || !isWritableFile(getApplication() , file)) return;

                    var archive = new File(path);
                    var archiveUnpack = new ArchiveUnpack(
                            getApplication(),
                            archive,
                            rootInDir
                    );

                    CompletableFuture
                            .runAsync(archiveUnpack::extractArchiveEntries, executor)
                            .thenRunAsync(() -> {
                                archive.delete();

                                var notificationBuild = new NotifyBuilder(getApplication(), UNPACK_GAME_CHANNEL_ID);
                                var unpackBody = getStockActivity().getString(R.string.bodyUnpackDoneNotify);
                                var notification = notificationBuild.buildStandardNotification(
                                        getStockActivity().getString(R.string.titleUnpackDoneNotify),
                                        unpackBody.replace("-GAMENAME-", currGameData.title)
                                );
                                var notificationManager = getApplication().getSystemService(NotificationManager.class);
                                notificationManager.notify(UNPACK_GAME_NOTIFICATION_ID, notification);

                                var localRemoteGame = new LocalGame(getApplication());
                                var gameFolder = archiveUnpack.unpackFolder;
                                localRemoteGame.createDataIntoFolder(currGameData, gameFolder);
                            }, executor)
                            .exceptionally(throwable -> {
                                Log.e(TAG, String.valueOf(throwable.getMessage()));
                                return null;
                            });
                }
            }
        }
    }

}
