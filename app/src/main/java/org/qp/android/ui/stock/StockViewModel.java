package org.qp.android.ui.stock;

import static org.qp.android.QuestopiaApplication.UNPACK_GAME_CHANNEL_ID;
import static org.qp.android.QuestopiaApplication.UNPACK_GAME_NOTIFICATION_ID;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.DirUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isSDCardAvailable;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.rxjava3.PagingRx;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.DocumentFileUtils;

import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.dto.stock.TempFile;
import org.qp.android.dto.stock.TempFileType;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
import org.qp.android.helpers.utils.DatabaseUtil;
import org.qp.android.helpers.utils.FileUtil;
import org.qp.android.model.archive.ArchiveUnpack;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.model.repository.LocalGame;
import org.qp.android.model.repository.RemoteGameSource;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;

@HiltViewModel
public class StockViewModel extends AndroidViewModel {

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;

    public static final String FOLDER_LOCK = "Select a folder location";
    public static final String FOLDER_CREATE = "Create a folder";

    public static final String GAME_DIR_NAME = "Questopia games";

    public final MutableLiveData<Integer> currPageNumber = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String TAG = this.getClass().getSimpleName();
    private final HashMap<Long, Game> gamesMap = new HashMap<>();
    private final DownloadManager downloadManager = getApplication().getSystemService(DownloadManager.class);
    private final LocalGame localGame;
    private final DatabaseUtil databaseUtil;
    private final FileCallback callback = new FileCallback() {
        @Override
        public void onConflict(@NonNull DocumentFile destinationFile,
                               @NonNull FileConflictAction action) {
            action.confirmResolution(ConflictResolution.REPLACE);
        }
    };
    public MutableLiveData<TempFile> fileMutableLiveData;
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;
    public boolean isEnableDeleteMode = false;
    public List<Game> selGameEntriesList = new ArrayList<>();
    public List<Game> currInstalledGamesList = new ArrayList<>();
    public Game currGameEntry;
    public MutableLiveData<Game> gameEntryLiveData = new MutableLiveData<>();
    public MutableLiveData<List<Game>> gameEntriesLiveData = new MutableLiveData<>();
    public final Events.Emitter actEmit = new Events.Emitter();
    public final Events.Emitter fragLocalRVEmit = new Events.Emitter();
    public final Events.Emitter fragRemoteRVEmit = new Events.Emitter();
    public Flowable<PagingData<Game>> remoteDataFlow;
    private Uri gameFolderUri;
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private StockDialogFrags dialogFragments = new StockDialogFrags();
    private long downloadId = 0L;

    @Inject
    public StockViewModel(@NonNull Application application,
                          @NonNull GameDao gameDao) {
        super(application);

        localGame = new LocalGame(gameDao, getApplication());
        databaseUtil = new DatabaseUtil(gameDao);

        initRecycler();
        checkFolder();
    }

    private void initRecycler() {
        var source = new RemoteGameSource();

        var pager = new Pager<>(new PagingConfig(
                10,
                10,
                false,
                5,
                10 * 499
        ), () -> source);

        remoteDataFlow = PagingRx.getFlowable(pager);
        var coroutineScope = ViewModelKt.getViewModelScope(this);
        PagingRx.cachedIn(remoteDataFlow, coroutineScope);
    }

    private void checkFolder() {
        // Questopia games (folder) -> Downloaded games (folder), .nomedia (file), .nosearch (file)
        // if is exist - do nothing
        // if is not exist - do show dialog -> Game folder is not found! Process to create?

        DocumentFile targetFolder = null;
        for (var file : ContextCompat.getExternalFilesDirs(getApplication(), null)) {
            var rootPath = file.getAbsolutePath();
            var storagePath = rootPath.replace("Android/data/org.qp.android/files", "");
            var rootFile = DocumentFile.fromFile(new File(storagePath));
            if (rootFile != null) {
                targetFolder = DocumentFileUtils.findFolder(rootFile, GAME_DIR_NAME);
                break;
            }
        }

        if (isWritableDir(getApplication(), targetFolder)) return;

        var inputStr = "No folder for downloading games was found. Download functionality is disabled";
        var rightButtonMsg = isSDCardAvailable(getApplication()) ? FOLDER_LOCK : FOLDER_CREATE;
        fragRemoteRVEmit.waitAndExecuteOnce(new StockFragmentNavigation.ShowErrorBanner(inputStr, rightButtonMsg));
    }

    // region Getter/Setter
    public void setCurrGameData(Game currGameData) {
        gameEntryLiveData.setValue(currGameData);
        this.currGameEntry = currGameData;
    }

    public void setTempPathFile(DocumentFile tempPathFile) {
        this.tempPathFile = tempPathFile;
        fileMutableLiveData.setValue(new TempFile(tempPathFile, TempFileType.PATH_FILE));
    }

    public void setTempModFile(DocumentFile tempModFile) {
        this.tempModFile = tempModFile;
        fileMutableLiveData.setValue(new TempFile(tempModFile, TempFileType.MOD_FILE));
    }

    public void setTempImageFile(@NonNull DocumentFile tempImageFile) {
        this.tempImageFile = tempImageFile;
        fileMutableLiveData.setValue(new TempFile(tempImageFile, TempFileType.IMAGE_FILE));
    }

    public List<Game> getListGames() {
        return new ArrayList<>(gamesMap.values());
    }

    public HashMap<Long, Game> getGamesMap() {
        return gamesMap;
    }

    public int getCountGameFiles() {
        var gameEntry = currGameEntry;
        if (gameEntry == null) return 0;
        return gameEntry.gameFilesUri.size();
    }

    @Nullable
    public Uri getGameFile(int index) {
        var gameEntry = currGameEntry;
        if (gameEntry == null) return null;
        return gameEntry.gameFilesUri.get(index);
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled() {
        var gameEntry = currGameEntry;
        if (gameEntry == null) return false;
        var gameDirUri = gameEntry.gameDirUri;
        if (gameDirUri == null) return false;
        var gameDir = DocumentFileCompat.fromUri(getApplication(), gameDirUri);
        return isDirContainsGameFile(getApplication(), gameDir);
    }

    public boolean isHasRemoteUrl() {
        var gameEntry = currGameEntry;
        if (gameEntry == null) return false;
        var fileUrl = currGameEntry.fileUrl;
        return isNotEmptyOrBlank(fileUrl);
    }

    // endregion Getter/Setter

    public boolean isModsDirExist() {
        var gameEntry = currGameEntry;
        if (gameEntry == null) return false;
        var gameDir = DocumentFileCompat.fromUri(getApplication(), gameEntry.gameDirUri);
        if (!isWritableDir(getApplication(), gameDir)) return false;
        var modDir = fromRelPath(getApplication(), "mods", gameDir, false);
        return isWritableFile(getApplication(), modDir);
    }

    public boolean doMakeGameDir(DocumentFile rootDir) {
        if (rootDir == null) {
            rootDir = DocumentFile.fromFile(Environment.getExternalStorageDirectory());
            if (!isWritableDir(getApplication(), rootDir)) return false;
            var gameFolder = FileUtil.findOrCreateFolder(getApplication(), rootDir, GAME_DIR_NAME);
            if (!isWritableDir(getApplication(), gameFolder)) return false;
            gameFolderUri = gameFolder.getUri();
            return true;
        }
        return false;
    }

    public void doOnShowFilePicker(int requestCode, String[] mimeTypes) {
        actEmit.waitAndExecute(new StockFragmentNavigation.ShowFilePicker(requestCode, mimeTypes));
    }

    public void doOnShowErrorDialog(String errorMessage, ErrorType errorType) {
        actEmit.waitAndExecute(new StockFragmentNavigation.ShowErrorDialog(errorMessage, errorType));
    }

    public void doOnShowDeleteDialog(String deleteMessage) {
        actEmit.waitAndExecuteOnce(new StockFragmentNavigation.ShowDeleteDialog(deleteMessage));
    }

    public void doOnShowGameFragment(Game entryToShow) {
        actEmit.waitAndExecute(new StockFragmentNavigation.ShowGameFragment(entryToShow));
    }

    public void doOnPrepareActionMode() {
        actEmit.waitAndExecute(new StockFragmentNavigation.PrepareActionMode());
    }

    public void doOnShowActionMode(ActionMode.Callback callback) {
        actEmit.waitAndExecute(new StockFragmentNavigation.ShowActionMode(callback));
    }

    public void doOnFinishActionMode() {
        actEmit.waitAndExecute(new StockFragmentNavigation.FinishActionMode());
    }

    public void doOnDestroyActionMode() {
        actEmit.waitAndExecute(new StockFragmentNavigation.DestroyActionMode());
    }

    public void doOnSelectAllElements() {
        fragLocalRVEmit.emitAndExecute(new StockFragmentNavigation.SelectAllElements());
    }

    public void doOnUnselectAllElements() {
        fragLocalRVEmit.emitAndExecute(new StockFragmentNavigation.UnselectAllElements());
    }

    public void onLongListItemClick() {
        if (isEnableDeleteMode) return;

        var pageNumber = currPageNumber.getValue();
        if (pageNumber == null) return;
        if (pageNumber == 1) return;

        var callback = new ActionMode.Callback() {
            Observer<Integer> observer = integer -> {
                if (integer == 1) {
                    removeEntryAndDirFromDB(selGameEntriesList);
                    doOnFinishActionMode();
                } else {
                    removeEntryFromDB(selGameEntriesList);
                    doOnFinishActionMode();
                }
            };

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                doOnPrepareActionMode();
                currInstalledGamesList = getListGames();
                isEnableDeleteMode = true;
                return true;
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                var gamesSelList = selGameEntriesList;
                var gameEntriesList = currInstalledGamesList;

                switch (item.getItemId()) {
                    case R.id.delete_game -> {
                        doOnShowDeleteDialog(String.valueOf(gamesSelList.size()));
                        outputIntObserver.observeForever(observer);
                    }
                    case R.id.select_all -> {
                        if (gamesSelList.size() == gameEntriesList.size()) {
                            gamesSelList.clear();
                            doOnUnselectAllElements();
                        } else {
                            gamesSelList.clear();
                            gamesSelList.addAll(gameEntriesList);
                            doOnSelectAllElements();
                        }
                    }
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                doOnDestroyActionMode();
                doOnUnselectAllElements();
                observer = null;
                isEnableDeleteMode = false;
                selGameEntriesList.clear();
                currInstalledGamesList.clear();
                doIsHideFAB.setValue(false);
            }
        };

        doIsHideFAB.setValue(true);

        doOnShowActionMode(callback);
    }

    // region Dialog
    public void showDialogFragment(FragmentManager manager,
                                   StockDialogType dialogType,
                                   String errorMessage,
                                   DocumentFile rootDir) {
        var fragment = manager.findFragmentByTag(dialogFragments.getTag());
        if (fragment != null && fragment.isAdded()) {
            fragment.onDestroy();
        } else {
            switch (dialogType) {
                case ADD_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.ADD_DIALOG);
                    dialogFragments.setNewDirEntry(rootDir);
                    dialogFragments.show(manager, "addDialogFragment");
                }
                case DELETE_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setMessage(errorMessage);
                    dialogFragments.setDialogType(StockDialogType.DELETE_DIALOG);
                    dialogFragments.show(manager, "deleteDialogFragment");
                }
                case EDIT_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
                    dialogFragments.show(manager, "editDialogFragment");
                }
                case ERROR_DIALOG -> {
                    var message = Optional.ofNullable(errorMessage);
                    dialogFragments.setDialogType(StockDialogType.ERROR_DIALOG);
                    message.ifPresent(s -> dialogFragments.setMessage(s));
                    dialogFragments.show(manager, "errorDialogFragment");
                }
                case MIGRATION_DIALOG -> {
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.MIGRATION_DIALOG);
                    dialogFragments.show(manager, "migrationDialogFragment");
                }
                case GAME_FOLDER_INIT -> {
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.GAME_FOLDER_INIT);
                    dialogFragments.show(manager, "gameFolderInitDialogFragment");
                }
                case SELECT_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    var names = new ArrayList<String>();
                    var files = currGameEntry.gameFilesUri.stream().map(uri -> DocumentFileCompat.fromUri(getApplication(), uri));
                    files.forEach(file -> names.add(file.getName()));
                    dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
                    dialogFragments.setNames(names);
                    dialogFragments.show(manager, "selectDialogFragment");
                }
            }
        }
    }

    public void createAddIntent(Game unfilledEntry, DocumentFile rootDir) {
        try {
            if (tempImageFile != null) unfilledEntry.gameIconUri = tempImageFile.getUri();

            localGame.insertEntryInDB(unfilledEntry, rootDir);
            outputIntObserver.setValue(1);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
        }
    }

    public void createEditIntent(Game unfilledEntry) {
        try {
            currGameEntry.title = unfilledEntry.title;
            currGameEntry.author = unfilledEntry.author;
            currGameEntry.version = unfilledEntry.version;

            if (tempImageFile != null) currGameEntry.gameIconUri = tempImageFile.getUri();

            var gameDir = DocumentFileCompat.fromUri(getApplication(), currGameEntry.gameDirUri);
            if (!isWritableDir(getApplication(), gameDir)) return;

            if (tempPathFile != null) {
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempPathFile, gameDir, callback
                        ));
            }

            if (tempModFile != null) {
                var modDir = fromRelPath(getApplication(), "mods", gameDir, false);
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(),
                                tempModFile, modDir, callback
                        ));
            }

            localGame.updateEntryInDB(currGameEntry)
                    .thenRun(this::loadGameDataFromDB)
                    .exceptionally(throwable -> {
                        doOnShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION);
                        return null;
                    });
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.toString(), ErrorType.EXCEPTION);
        }
    }

    // TODO: 25.06.2024 Release service for calculate size dir
    private void calculateSizeDir() {
//        var gameDir = gameData.getGameDir(getApplication());

//        CompletableFuture
//                .supplyAsync(() -> calculateDirSize(gameDir), executor)
//                .thenAccept(aLong -> {
//                    gameData.fileSize = formatFileSize(aLong, getController().binaryPrefixes);
//                    localGame.updateEntryInDB(gameData);
//                    gameData.fileSize = String.valueOf(aLong);
//                });
    }

    public Intent createPlayGameIntent() {
        var data = currGameEntry;
        if (data == null) return null;
        var gameDir = DocumentFileCompat.fromUri(getApplication(), currGameEntry.gameDirUri);
        if (!isWritableDir(getApplication(), gameDir)) return null;
        var intent = new Intent(getApplication(), GameActivity.class);

        var application = (QuestopiaApplication) getApplication();
        application.currentGameDir = gameDir;

        intent.putExtra("gameId", data.id);
        intent.putExtra("gameTitle", data.title);
        intent.putExtra("gameDirUri", String.valueOf(gameDir.getUri()));

        return intent;
    }

    // endregion Dialog
    public void createEntryInDBFromFile(DocumentFile gameDir) {
        localGame.createEntryInDBFromDir(gameDir)
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void loadGameDataFromDB() {
        gamesMap.clear();

        databaseUtil.getAllGameEntries()
                .thenAcceptAsync(listGameEntries -> {
                    listGameEntries.forEach(gameData -> gamesMap.put(gameData.id, gameData));
                    gameEntriesLiveData.postValue(new ArrayList<>(gamesMap.values()));
                }, executor)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error: ", throwable);
                    return null;
                });
    }

    private void dropPersistable(Uri folderUri) {
        try {
            var contentResolver = getApplication().getContentResolver();
            contentResolver.releasePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }
    }

    public CompletableFuture<Void> removeEntryFromDB(List<Game> entriesDelete) {
        return localGame.deleteGamesEntries(entriesDelete)
                .thenAccept(integer -> {
                    for (var element : entriesDelete) {
                        gamesMap.remove(element.id, element);
                    }
                    var localGameData = new ArrayList<>(gamesMap.values());
                    gameEntriesLiveData.postValue(localGameData);
                    loadGameDataFromDB();
                });
    }

    public void removeEntryAndDirFromDB(List<Game> entriesDelete) {
        removeEntryFromDB(entriesDelete)
                .thenRunAsync(() -> {
                    for (var element : entriesDelete) {
                        var folder = DocumentFileCompat.fromUri(getApplication(), element.gameDirUri);
                        if (folder == null) return;
                        var doDelete = DocumentFileUtils.forceDelete(folder, getApplication());
                        if (!doDelete) return;
                        dropPersistable(element.gameDirUri);
                    }
                }, executor);
    }

    // Download game
    public void startFileDownload(Game gameEntry) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var convUrl = new URL(gameEntry.fileUrl);

                        var cookie = CookieManager.getInstance().getCookie(gameEntry.fileUrl);
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
                    if (isNotEmptyOrBlank(s)) return;

                    var downloadUri = Uri.parse(gameEntry.fileUrl);
                    var request = new DownloadManager.Request(downloadUri)
                            .setVisibleInDownloadsUi(true)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, s);
                    downloadId = downloadManager.enqueue(request);
                })
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.getMessage(), ErrorType.EXCEPTION);
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
                    var file = DocumentFileCompat.fromUri(getApplication(), Uri.parse(c.getString(colUriIndex)));
                    if (!isWritableFile(getApplication(), file)) return;

                    var archive = new File(path);
                    var archiveUnpack = new ArchiveUnpack(
                            getApplication(),
                            file.getUri(),
                            gameFolderUri
                    );

                    CompletableFuture
                            .runAsync(archiveUnpack::extractArchiveEntries, executor)
                            .thenRunAsync(() -> {
                                Log.i(TAG, "Archive is delete " + archive.delete());

                                var notificationBuild = new NotifyBuilder(getApplication(), UNPACK_GAME_CHANNEL_ID);
                                var unpackBody = ActivityCompat.getString(getApplication(), R.string.bodyUnpackDoneNotify);
                                var notification = notificationBuild.buildStandardNotification(
                                        ActivityCompat.getString(getApplication(), R.string.titleUnpackDoneNotify),
                                        unpackBody.replace("-GAMENAME-", currGameEntry.title)
                                );
                                var notificationManager = getApplication().getSystemService(NotificationManager.class);
                                notificationManager.notify(UNPACK_GAME_NOTIFICATION_ID, notification);

                                var gameFolderUri = archiveUnpack.unpackFolder;
                                var gameFolder = DocumentFileCompat.fromUri(getApplication(), gameFolderUri);
                                if (isWritableDir(getApplication(), gameFolder)) {
                                    createEntryInDBFromFile(gameFolder);
                                }
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

//public void startMigration() {
//    var cache = getApplication().getExternalCacheDir();
//    var listDirsFile = new File(cache , "tempListDir");
//
//    if (!listDirsFile.exists()) return;
//
//    try {
//        var ref = new TypeReference<HashMap<String , String>>() {};
//        var mapFiles = jsonToObject(listDirsFile , ref);
//        var listFile = new ArrayList<DocumentFile>();
//        for (var value : mapFiles.values()) {
//            var uri = Uri.parse(value);
//            var file = DocumentFileCompat.fromUri(getApplication() , uri);
//            listFile.add(file);
//        }
//        endMigration(listFile);
//    } catch (IOException e) {
//        Log.e(TAG , "Error: ", e);
//    }
//}
//
//private void endMigration(ArrayList<DocumentFile> listGamesDir) {
//    CompletableFuture
//            .supplyAsync(() -> localGame.extractDataFromList(getApplication() , listGamesDir), executor)
//            .thenApply(gameDataList -> {
//                var listGame = new ArrayList<Game>();
//
//                gameDataList.forEach(gameData -> {
//                    var emptyGameEntry = new Game();
//
//                    emptyGameEntry.id = gameData.id;
//                    emptyGameEntry.author = gameData.author;
//                    emptyGameEntry.portedBy = gameData.portedBy;
//                    emptyGameEntry.version = gameData.version;
//                    emptyGameEntry.title = gameData.title;
//                    emptyGameEntry.lang = gameData.lang;
//                    emptyGameEntry.player = gameData.player;
//                    emptyGameEntry.icon = gameData.icon;
//                    emptyGameEntry.fileUrl = gameData.fileUrl;
//                    emptyGameEntry.fileSize = DISABLE_CALCULATE_DIR;
//                    emptyGameEntry.fileExt = gameData.fileExt;
//                    emptyGameEntry.descUrl = gameData.descUrl;
//                    emptyGameEntry.pubDate = gameData.pubDate;
//                    emptyGameEntry.modDate = gameData.modDate;
//                    emptyGameEntry.gameDirUri = gameData.gameDir.getUri();
//
//                    var gameUriList = new ArrayList<Uri>();
//                    gameData.gameFiles.forEach(documentFile ->
//                            gameUriList.add(documentFile.getUri()));
//                    emptyGameEntry.gameFilesUri = gameUriList;
//
//                    listGame.add(emptyGameEntry);
//                });
//                return listGame;
//            })
//            .thenAcceptAsync(gameDao::insertAll, executor)
//            .thenRun(this::loadGameDataFromDB);
//}
