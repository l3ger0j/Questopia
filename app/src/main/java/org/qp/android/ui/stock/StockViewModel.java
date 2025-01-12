package org.qp.android.ui.stock;

import static org.qp.android.QuestopiaApplication.UNPACK_GAME_CHANNEL_ID;
import static org.qp.android.QuestopiaApplication.UNPACK_GAME_NOTIFICATION_ID;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.DirUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

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
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.rxjava3.PagingRx;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.squareup.picasso.Picasso;

import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.data.db.GameDatabase;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.EventEmitter;
import org.qp.android.helpers.utils.DatabaseUtil;
import org.qp.android.model.archive.ArchiveUnpack;
import org.qp.android.model.notify.NotifyBuilder;
import org.qp.android.model.repository.LocalGame;
import org.qp.android.model.repository.RemoteGamePagingSource;
import org.qp.android.ui.dialogs.StockDialogFrags;
import org.qp.android.ui.dialogs.StockDialogType;
import org.qp.android.ui.game.GameActivity;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
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

    public static final String DISABLE_CALCULATE_DIR = "disable";
    public static final String EXT_GAME_LIST_NAME = "extGameDirs";
    private static final String INNER_GAME_DIR_NAME = "games-dir";
    public final MutableLiveData<Integer> currPageNumber = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final String TAG = this.getClass().getSimpleName();
    private final HashMap<Long, Game> gamesMap = new HashMap<>();
    private final DownloadManager downloadManager = getApplication().getSystemService(DownloadManager.class);
    private final LocalGame localGame;
    private final DatabaseUtil databaseUtil;
    private final File rootInDir;
    private final GameDatabase gameDatabase;
    private final GameDao gameDao;
    private final FileCallback callback = new FileCallback() {
        @Override
        public void onConflict(@NonNull DocumentFile destinationFile,
                               @NonNull FileConflictAction action) {
            action.confirmResolution(ConflictResolution.REPLACE);
        }
    };
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;
    public boolean isEnableDeleteMode = false;
    public List<Game> selGameEntriesList = new ArrayList<>();
    public List<Game> currInstalledGamesList = new ArrayList<>();
    public Game currGameEntry;
    public MutableLiveData<Game> gameEntryLiveData = new MutableLiveData<>();
    public MutableLiveData<List<Game>> gameEntriesLiveData = new MutableLiveData<>();
    public EventEmitter emitter = new EventEmitter();
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private DialogEditBinding editBinding;
    private DialogAddBinding addBinding;
    private StockDialogFrags dialogFragments = new StockDialogFrags();
    private long downloadId = 0L;

    public Flowable<PagingData<Game>> remoteDataFlow;

    @Inject
    public StockViewModel(@NonNull Application application,
                          @NonNull GameDao gameDao,
                          @NonNull GameDatabase gameDatabase) {
        super(application);

        this.gameDao = gameDao;
        this.gameDatabase = gameDatabase;

        localGame = new LocalGame(gameDao, getApplication());
        databaseUtil = new DatabaseUtil(gameDao);

        var rootInDir = getApplication().getExternalFilesDir(null);
        this.rootInDir = findOrCreateFolder(getApplication(), rootInDir, INNER_GAME_DIR_NAME);

        initRecycler();
    }

    private void initRecycler() {
        var source = new RemoteGamePagingSource();

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

    // region Getter/Setter
    public void setCurrGameData(Game currGameData) {
        gameEntryLiveData.setValue(currGameData);
        this.currGameEntry = currGameData;
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
        if (addBinding != null) {
            addBinding.buttonSelectIcon.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(addBinding.imageView);
        }
        if (editBinding != null) {
            editBinding.buttonSelectIcon.setText(tempImageFile.getName());
            Picasso.get()
                    .load(tempImageFile.getUri())
                    .fit()
                    .into(editBinding.iconView);
        }
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

    public void doOnShowFilePicker(int requestCode, String[] mimeTypes) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowFilePicker(requestCode, mimeTypes));
    }

    public void doOnShowErrorDialog(String errorMessage, ErrorType errorType) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowErrorDialog(errorMessage, errorType));
    }

    public void doOnShowGameFragment(int position) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowGameFragment(position));
    }

    public void doOnShowGameFragment(Game entryToShow) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowGameFragment(entryToShow));
    }

    public void doOnShowActionMode() {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowActionMode());
    }

    public void doOnSelectAllElements() {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.SelectAllElements());
    }

    public void doOnUnselectAllElements() {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.UnselectAllElements());
    }

    // region Dialog
    public void showAddDialogFragment(FragmentManager manager,
                                      DocumentFile rootDir) {
        outputIntObserver = new MutableLiveData<>();
        dialogFragments = new StockDialogFrags();
        dialogFragments.setDialogType(StockDialogType.ADD_DIALOG);
        dialogFragments.setAddBinding(setupAddView(rootDir));
        dialogFragments.show(manager, "addDialogFragment");
    }

    public void showDialogFragment(FragmentManager manager,
                                   StockDialogType dialogType,
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
                    dialogFragments.show(manager, "deleteDialogFragment");
                }
                case EDIT_DIALOG -> {
                    dialogFragments = new StockDialogFrags();
                    dialogFragments.setDialogType(StockDialogType.EDIT_DIALOG);
                    dialogFragments.setEditBinding(buildEditDialog());
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

            var newGameEntry = new Game();
            var editTextTitle = addBinding.ET0.getEditText();
            if (editTextTitle != null) {
                newGameEntry.title = editTextTitle.getText().toString().isEmpty()
                        ? nameDir
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
                newGameEntry.gameIconUri = tempImageFile.getUri();
            }
            newGameEntry.fileSize = DISABLE_CALCULATE_DIR;
            databaseUtil.updateOrInsertEntry(newGameEntry);
            outputIntObserver.setValue(1);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
        }
    }

    @NonNull
    private DialogEditBinding buildEditDialog() {
        editBinding = DialogEditBinding.inflate(LayoutInflater.from(getApplication()));

        var gameDataObserver = new GameDataObserver();
        gameDataObserver.isModDirExist.set(isModsDirExist());
        gameDataObserver.iconUriObserver.set(currGameEntry.gameIconUri);
        editBinding.setGame(gameDataObserver);

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
                currGameEntry.title = editTextTitle.getText().toString().isEmpty()
                        ? removeExtension(currGameEntry.title)
                        : editTextTitle.getText().toString();
            }

            var editTextAuthor = editBinding.ET1.getEditText();
            if (editTextAuthor != null) {
                currGameEntry.author = editTextAuthor.getText().toString().isEmpty()
                        ? removeExtension(currGameEntry.author)
                        : editTextAuthor.getText().toString();
            }

            var editTextVersion = editBinding.ET2.getEditText();
            if (editTextVersion != null) {
                currGameEntry.version = editTextVersion.toString().isEmpty()
                        ? removeExtension(currGameEntry.version)
                        : editTextVersion.getText().toString();
            }

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
                    .thenRun(this::loadGameDataFromDB);
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
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
        application.setCurrentGameDir(gameDir);

        intent.putExtra("gameId", data.id);
        intent.putExtra("gameTitle", data.title);
        intent.putExtra("gameDirUri", String.valueOf(gameDir.getUri()));

        return intent;
    }

    @SuppressLint("NonConstantResourceId")
    public void sendIntent(@NonNull View view) {
        switch (view.getId()) {
            case R.id.buttonSelectIcon ->
                    doOnShowFilePicker(CODE_PICK_IMAGE_FILE, new String[]{"image/png", "image/jpeg"});
            case R.id.buttonSelectPath ->
                    doOnShowFilePicker(CODE_PICK_PATH_FILE, new String[]{"application/octet-stream"});
            case R.id.buttonSelectMod ->
                    doOnShowFilePicker(CODE_PICK_MOD_FILE, new String[]{"application/octet-stream"});
        }
    }

    // endregion Dialog
    public void createEntryInDBFromFile(DocumentFile gameDir) {
        localGame.createEntryInDBFromDir(gameDir)
                .thenRun(this::loadGameDataFromDB)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error: ", throwable);
                    return null;
                });
    }

    //                        if (!data.isFileInstalled()) continue;
//                        if (!isNotEmpty(data.fileSize)) {
//                            calculateSizeDir(data);
//                        } else {
//                            if (!data.fileSize.equals(DISABLE_CALCULATE_DIR)) {
//                                try {
//                                    var fileSize = Long.parseLong(data.fileSize);
//                                    var currPrefix = getController().binaryPrefixes;
//                                    data.fileSize = formatFileSize(fileSize , currPrefix);
//                                } catch (NumberFormatException ignored) {}
//                            }
//                        }

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

    public void refreshGameEntry() {
        gamesMap.clear();

        var pageNumber = currPageNumber.getValue();
        if (pageNumber == null) return;

        if (pageNumber == 0) {
            gameEntriesLiveData.postValue(Collections.emptyList());

            doIsHideFAB.setValue(false);

            databaseUtil.getAllGameEntries()
                    .thenAcceptAsync(listGameEntries -> {
                        listGameEntries.forEach(gameData -> {
                            if (gameData.listId == 0) {
                                gamesMap.put(gameData.id, gameData);
                            }
                        });
                        gameEntriesLiveData.postValue(new ArrayList<>(gamesMap.values()));
                    }, executor)
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Error: ", throwable);
                        return null;
                    });
        }
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

    public void removeEntryFromDB(List<Game> entriesDelete) {
        localGame.deleteGamesEntries(entriesDelete)
                .thenAccept(integer -> {
                    for (var element : entriesDelete) {
                        gamesMap.remove(element.id, element);
                    }
                    var localGameData = new ArrayList<>(gamesMap.values());
                    gameEntriesLiveData.postValue(localGameData);
                    refreshGameEntry();
                });
    }

    public void removeEntryAndDirFromDB(List<Game> entriesDelete) {

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
                    if (s.isEmpty() || s.isBlank()) return;

                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .mkdirs();

                    var downloadUri = Uri.parse(gameEntry.fileUrl);
                    var request = new DownloadManager.Request(downloadUri)
                            .setVisibleInDownloadsUi(true)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalFilesDir(getApplication(), Environment.DIRECTORY_DOWNLOADS, s);
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
                    if (file == null || !isWritableFile(getApplication(), file)) return;

                    var archive = new File(path);
                    var archiveUnpack = new ArchiveUnpack(
                            getApplication(),
                            archive,
                            rootInDir
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

                                var gameFolder = archiveUnpack.unpackFolder;
                                localGame.createEntryInDBFromDir(gameFolder);
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
