package org.qp.android.ui.stock;

import static org.qp.android.QuestopiaApplication.UNPACK_GAME_CHANNEL_ID;
import static org.qp.android.QuestopiaApplication.UNPACK_GAME_NOTIFICATION_ID;
import static org.qp.android.helpers.utils.DirUtil.MOD_DIR_NAME;
import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.DirUtil.getNamesDir;
import static org.qp.android.helpers.utils.DirUtil.isDirContainsGameFile;
import static org.qp.android.helpers.utils.DirUtil.isModDirExist;
import static org.qp.android.helpers.utils.FileUtil.copyFileToDir;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFolder;
import static org.qp.android.helpers.utils.FileUtil.forceDelFile;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;
import static org.qp.android.helpers.utils.PathUtil.removeExtension;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.runOnUiThread;
import static org.qp.android.helpers.utils.XmlUtil.xmlToObject;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.squareup.picasso.Picasso;

import org.ocpsoft.prettytime.PrettyTime;
import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.databinding.DialogAddBinding;
import org.qp.android.databinding.DialogEditBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.dto.stock.RemoteGameData;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.Events;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockViewModel extends AndroidViewModel {

    public static final int CODE_PICK_IMAGE_FILE = 300;
    public static final int CODE_PICK_PATH_FILE = 301;
    public static final int CODE_PICK_MOD_FILE = 302;
    public static final long DISABLE_CALC_SIZE = -1;
    public static final String EXT_GAME_LIST_NAME = "extGameDirs";
    private static final String INNER_GAME_DIR_NAME = "games-dir";
    public final MutableLiveData<Integer> currPageNumber = new MutableLiveData<>();
    public final MutableLiveData<List<GameData>> remoteDataList = new MutableLiveData<>();
    public final MutableLiveData<List<GameData>> localDataList = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    // Containers
    private final HashMap<Long, GameData> gamesMap = new HashMap<>();
    private final LocalGame localGame = new LocalGame(getApplication());
    private final DownloadManager downloadManager = getApplication().getSystemService(DownloadManager.class);
    public final File listDirsFile;
    private final File rootInDir;
    private final FileCallback callback = new FileCallback() {
        @Override
        public void onConflict(@NonNull DocumentFile destinationFile,
                               @NonNull FileConflictAction action) {
            action.confirmResolution(ConflictResolution.REPLACE);
        }
    };
    public MutableLiveData<Boolean> doIsHideFAB = new MutableLiveData<>();
    public MutableLiveData<Integer> outputIntObserver;
    public List<DocumentFile> extGamesListDir = new ArrayList<>();
    public GameData currGameData;
    public Events.Emitter emitter = new Events.Emitter();
    protected boolean isEnableDeleteMode = false;
    protected final List<GameData> tempList = new ArrayList<>();
    protected final List<GameData> selectList = new ArrayList<>();
    private DocumentFile tempImageFile, tempPathFile, tempModFile;
    private DialogEditBinding editBinding;
    private DialogAddBinding addBinding;
    private StockDialogFrags dialogFragments = new StockDialogFrags();
    private long downloadId = 0L;

    public StockViewModel(@NonNull Application application) {
        super(application);

        var cache = getApplication().getExternalCacheDir();
        listDirsFile = findOrCreateFile(getApplication(), cache, EXT_GAME_LIST_NAME, MimeType.TEXT);

        var rootInDir = getApplication().getExternalFilesDir(null);
        this.rootInDir = findOrCreateFolder(getApplication(), rootInDir, INNER_GAME_DIR_NAME);
    }

    // region Getter/Setter
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
            editBinding.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            editBinding.imageView.setImageURI(tempImageFile.getUri());
        }
        if (addBinding != null) {
            addBinding.buttonSelectIcon.setText(tempImageFile.getName());
            addBinding.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            addBinding.imageView.setImageURI(tempImageFile.getUri());
        }
    }

    public void setDataList(List<GameData> insertList) {
        if (localDataList.hasActiveObservers()) {
            localDataList.setValue(insertList);
        }
        if (remoteDataList.hasActiveObservers()) {
            remoteDataList.setValue(insertList);
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
        gameData.sort(Comparator.comparing(game -> game.title.toLowerCase()));
        gameData.sort(Comparator.comparing(game -> game.listId));
        return gameData;
    }

    public Optional<GameData> getCurrGameData() {
        return Optional.ofNullable(currGameData);
    }

    public HashMap<Long, GameData> getGamesMap() {
        return gamesMap;
    }

    public String getGameTitle() {
        var data = currGameData;
        if (data == null) return "";

        var title = data.title;
        if (!isNotEmptyOrBlank(title)) return "";

        return title;
    }

    public String getGameAuthor() {
        var data = currGameData;
        if (data == null) return "";

        var author = data.author;
        if (!isNotEmptyOrBlank(author)) return "";

        var authorString = ActivityCompat.getString(getApplication(), R.string.author);
        return authorString.replace("-AUTHOR-", author);
    }

    public Uri getGameIcon() {
        var data = currGameData;
        if (data == null) return Uri.EMPTY;

        var icon = data.iconUrl;
        if (!isNotEmptyOrBlank(String.valueOf(icon))) return Uri.EMPTY;

        return icon;
    }

    public String getGamePortBy() {
        var data = currGameData;
        if (data == null) return "";

        var portedBy = data.portedBy;
        if (!isNotEmptyOrBlank(portedBy)) return "";

        var portedByString = ActivityCompat.getString(getApplication(), R.string.ported_by);
        return portedByString.replace("-PORTED_BY-", portedBy);
    }

    public String getGameVersion() {
        var data = currGameData;
        if (data == null) return "";

        var version = data.version;
        if (!isNotEmptyOrBlank(version)) return "";

        var versionString = ActivityCompat.getString(getApplication(), R.string.version);
        return versionString.replace("-VERSION-", version);
    }

    public String getGameType() {
        var data = currGameData;
        if (data == null) return "";

        var fileExt = data.fileExt;
        if (!isNotEmptyOrBlank(fileExt)) return "";

        var fileTypeSting = ActivityCompat.getString(getApplication(), R.string.fileType);
        if (fileExt.equals("aqsp")) {
            var experimentalString = ActivityCompat.getString(getApplication(), R.string.experimental);
            return fileTypeSting.replace("-TYPE-", fileExt) + " " + experimentalString;
        } else {
            return fileTypeSting.replace("-TYPE-", fileExt);
        }
    }

    public long getGameSize() {
        var data = currGameData;
        if (data == null) return 0L;
        return data.fileSize;
    }

    public String getFormattedGameSize() {
        var data = currGameData;
        if (data == null) return "";

        var fileSize = data.fileSize;
        if (fileSize == DISABLE_CALC_SIZE) return "";

        var currBinPref = getController().binaryPrefixes;
        var sizeWithPref = formatFileSize(fileSize, currBinPref);

        var fileSizeString = ActivityCompat.getString(getApplication(), R.string.fileSize);
        return fileSizeString.replace("-SIZE-", sizeWithPref);
    }

    public boolean isPubModDataExist() {
        return isNotEmptyOrBlank(getGamePubData()) || isNotEmptyOrBlank(getGameModData());
    }

    public String getGamePubData() {
        var data = currGameData;
        if (data == null) return "";

        var pubDate = data.pubDate;
        if (!isNotEmptyOrBlank(pubDate)) return "";

        var p = new PrettyTime();
        var parse = LocalDateTime.parse(pubDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        var pubDataString = ActivityCompat.getString(getApplication(), R.string.pub_data);
        return pubDataString.replace("-PUB_DATA-", p.format(parse));
    }

    public String getGameModData() {
        var data = currGameData;
        if (data == null) return "";

        var modDate = data.modDate;
        if (!isNotEmptyOrBlank(modDate)) return "";

        var p = new PrettyTime();
        var parse = LocalDateTime.parse(modDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        var modDataString = ActivityCompat.getString(getApplication(), R.string.mod_data);
        return modDataString.replace("-MOD_DATA-", p.format(parse));
    }

    public int getCountGameFiles() {
        var gameData = currGameData;
        if (gameData == null) return 0;
        return gameData.gameFilesUri.size();
    }

    @NonNull
    public Uri getGameFile(int index) {
        var gameData = currGameData;
        if (gameData == null) return Uri.EMPTY;
        return gameData.gameFilesUri.get(index);
    }

    public boolean isGamePossiblyDownload() {
        return !isGameInstalled() && isHasRemoteUrl();
    }

    public boolean isGameInstalled() {
        var gameData = currGameData;
        if (gameData == null) return false;
        var rootDirUri = gameData.gameDirUri;
        if (rootDirUri == Uri.EMPTY) return false;
        if (isNotEmptyOrBlank(String.valueOf(rootDirUri))) {
            return isDirContainsGameFile(getApplication(), rootDirUri);
        }
        return false;
    }

    public boolean isGameInstalled(GameData entry) {
        if (entry == null) return false;
        var rootDirUri = entry.gameDirUri;
        if (rootDirUri == Uri.EMPTY) return false;
        if (!isNotEmptyOrBlank(String.valueOf(rootDirUri))) return false;
        return isDirContainsGameFile(getApplication(), rootDirUri);
    }

    public boolean isHasRemoteUrl() {
        var gameData = currGameData;
        if (gameData == null) return false;
        return isNotEmptyOrBlank(currGameData.fileUrl);
    }

    public boolean isModsDirExist() {
        var gameData = currGameData;
        if (gameData == null) return false;
        return isModDirExist(getApplication(), currGameData.gameDirUri);
    }

    // endregion Getter/Setter

    public void doOnShowFilePicker(int requestCode, String[] mimeTypes) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowFilePicker(requestCode, mimeTypes));
    }

    public void doOnShowErrorDialog(String errorMessage, ErrorType errorType) {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ShowErrorDialog(errorMessage, errorType));
    }

    public void doOnShowDeleteDialog(String errorMessage) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowDeleteDialog(errorMessage));
    }

    public void doOnShowActionMode(ActionMode.Callback callback) {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.ShowActionMode(callback));
    }

    public void doOnFinishActionMode() {
        emitter.waitAndExecuteOnce(new StockFragmentNavigation.FinishActionMode());
    }

    public void doOnChangeDestination(@IdRes int resId) {
        emitter.emitAndExecute(new StockFragmentNavigation.ChangeDestination(resId));
    }

    public void doOnChangeElementColorToDKGray() {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ChangeElementColorToDKGray());
    }

    public void doOnChangeElementColorToLTGray() {
        emitter.emitAndExecuteOnce(new StockFragmentNavigation.ChangeElementColorToLTGray());
    }

    public void onListItemClick(GameData entryToShow) {
        if (isEnableDeleteMode) return;
        currGameData = entryToShow;
        doOnChangeDestination(R.id.stockGameFragment);
        doIsHideFAB.setValue(true);
    }

    public void onListItemClick(int position) {
        if (!isEnableDeleteMode) return;
        CompletableFuture
                .runAsync(() -> {
                    var curMapValues = gamesMap.values();

                    for (var gameData : curMapValues) {
                        if (!isGameInstalled(gameData)) continue;
                        tempList.add(gameData);
                    }
                }, executor)
                .thenRun(() -> {
                    var gameData = tempList.get(position);
                    if (selectList.isEmpty() || !selectList.contains(gameData)) {
                        selectList.add(gameData);
                        emitter.waitAndExecuteOnce(new StockFragmentNavigation.SelectOnce(position));
                    } else {
                        selectList.remove(gameData);
                        emitter.waitAndExecuteOnce(new StockFragmentNavigation.UnselectOnce(position));
                    }
                });
    }

    public void onLongListItemClick() {
        if (isEnableDeleteMode) return;

        var pageNumber = currPageNumber.getValue();
        if (pageNumber == null) return;
        if (pageNumber == 1) return;

        var callback = new ActionMode.Callback() {
            Observer<Integer> observer = integer -> {
                if (integer == 1) {
                    for (var data : selectList) {
                        delEntryDirFromList(tempList, data, listDirsFile);
                    }
                    doOnFinishActionMode();
                } else {
                    for (var data : selectList) {
                        delEntryFromList(tempList, data, listDirsFile);
                    }
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
                tempList.addAll(getSortedGames());
                isEnableDeleteMode = true;
                return true;
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.delete_game -> {
                        doOnShowDeleteDialog(String.valueOf(selectList.size()));
                        outputIntObserver.observeForever(observer);
                    }
                    case R.id.select_all -> {
                        if (selectList.size() == tempList.size()) {
                            selectList.clear();
                            doOnChangeElementColorToDKGray();
                        } else {
                            selectList.clear();
                            selectList.addAll(tempList);
                            doOnChangeElementColorToLTGray();
                        }
                    }
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                doOnChangeElementColorToDKGray();
                isEnableDeleteMode = false;
                tempList.clear();
                selectList.clear();
                doIsHideFAB.setValue(false);
                observer = null;
            }
        };

        doIsHideFAB.setValue(true);

        doOnShowActionMode(callback);
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
                    dialogFragments.setEditBinding(formingEditView());
                    dialogFragments.show(manager, "editDialogFragment");
                }
                case ERROR_DIALOG -> {
                    var message = Optional.ofNullable(errorMessage);
                    dialogFragments.setDialogType(StockDialogType.ERROR_DIALOG);
                    message.ifPresent(s -> dialogFragments.setMessage(s));
                    dialogFragments.show(manager, "errorDialogFragment");
                }
                case SELECT_DIALOG -> {
                    outputIntObserver = new MutableLiveData<>();
                    dialogFragments.setDialogType(StockDialogType.SELECT_DIALOG);
                    dialogFragments.setNames(getNamesDir(getApplication(), currGameData.gameFilesUri));
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
            var secureRandom = new SecureRandom();

            if (!isNotEmptyOrBlank(nameDir)) {
                nameDir = "game#" + secureRandom.nextInt();
            }

            var newGameData = new GameData();
            newGameData.id = secureRandom.nextInt();
            var editTextTitle = addBinding.ET0.getEditText();
            if (editTextTitle != null) {
                var title = editTextTitle.getText().toString();
                newGameData.title = isNotEmptyOrBlank(title)
                        ? title
                        : nameDir;
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
                newGameData.iconUrl = tempImageFile.getUri();
            }
            if (addBinding.sizeDirSW.isChecked()) {
                calculateSizeDir(newGameData);
            } else {
                newGameData.fileSize = DISABLE_CALC_SIZE;
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

        var data = currGameData;
        if (data != null) {
            var iconPath = data.iconUrl;
            if (isNotEmptyOrBlank(String.valueOf(iconPath))) {
                Picasso.get()
                        .load(iconPath)
                        .fit()
                        .into(editBinding.imageView);
            }
        }

        if (getGameSize() == DISABLE_CALC_SIZE) {
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
            if (tempImageFile != null) currGameData.iconUrl = tempImageFile.getUri();
            if (editBinding.sizeDirSW.isChecked() || getGameSize() != DISABLE_CALC_SIZE) {
                calculateSizeDir(currGameData);
            }

            var gameDir = DocumentFileCompat.fromUri(getApplication(), currGameData.gameDirUri);
            if (isWritableFile(getApplication(), tempPathFile) && isWritableDir(getApplication(), gameDir)) {
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(), tempPathFile, gameDir, callback));
            }
            if (isWritableFile(getApplication(), tempModFile) && isWritableDir(getApplication(), gameDir)) {
                var modDir = fromRelPath(getApplication(), MOD_DIR_NAME, gameDir);
                CompletableFuture
                        .runAsync(() -> copyFileToDir(getApplication(), tempModFile, modDir, callback));
            }

            localGame.createDataIntoFolder(currGameData, gameDir);
            refreshGamesDirs();
            dialogFragments.dismiss();
        } catch (NullPointerException ex) {
            doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
        }
    }

    private void calculateSizeDir(GameData gameData) {
        var gameDir = DocumentFileCompat.fromUri(getApplication(), gameData.gameDirUri);
        if (!isWritableDir(getApplication(), gameDir)) return;

        CompletableFuture
                .supplyAsync(() -> calculateDirSize(gameDir), executor)
                .thenAccept(aLong -> {
                    gameData.fileSize = aLong;
                    localGame.createDataIntoFolder(gameData, gameDir);
                });
    }

    public Optional<Intent> createPlayGameIntent() {
        if (getCurrGameData().isPresent()) {
            var data = getCurrGameData().get();
            var gameDir = DocumentFileCompat.fromUri(getApplication(), currGameData.gameDirUri);
            if (!isWritableDir(getApplication(), gameDir)) return Optional.empty();
            var intent = new Intent(getApplication(), GameActivity.class);

            var application = (QuestopiaApplication) getApplication();
            application.setCurrentGameDir(gameDir);

            intent.putExtra("gameId", data.id);
            intent.putExtra("gameTitle", data.title);
            intent.putExtra("gameDirUri", String.valueOf(gameDir.getUri()));

            return Optional.of(intent);
        } else {
            return Optional.empty();
        }
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

    // region Refresh
    public void refreshGamesDirs() {
        var rootExDir = ((QuestopiaApplication) getApplication()).getCurrentGameDir();

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
                        throw new CompletionException(e);
                    }
                }, executor);
    }

    private CompletableFuture<List<GameData>> fetchExternalData() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return localGame.extractDataFromList(extGamesListDir);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
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
                            try {
                                var ref = new TypeReference<List<RemoteGameData>>() {};
                                return xmlToObject(file, ref);
                            } catch (IOException e) {
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
            doIsHideFAB.setValue(false);
            syncFromDisk();
        }

        if (pageNumber == 1) {
            doIsHideFAB.setValue(true);
            syncRemote();
        }
    }

    private void syncFromDisk() {
        fetchInternalData()
                .thenCombineAsync(fetchExternalData(), (intDataList, extDataList) -> {
                    intDataList.addAll(extDataList);
                    return intDataList;
                }, executor)
                .thenAcceptAsync(externalGameData -> externalGameData.forEach(localGameData -> gamesMap.put(localGameData.id, localGameData)), executor)
                .thenApplyAsync(x -> {
                    var syncDataList = Collections.synchronizedCollection(gamesMap.values());
                    if (syncDataList.size() < 2) return syncDataList;
                    return syncDataList.stream()
                            .sorted(Comparator.comparing(game -> game.title.toLowerCase()))
                            .sorted(Comparator.comparing(game -> game.listId))
                            .toList();
                }, executor)
                .thenAcceptAsync(list -> localDataList.postValue(list.stream().filter(this::isGameInstalled).toList()), executor)
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    private void syncRemote() {
        fetchInternalData()
                .thenCombineAsync(fetchRemoteData(), (intDataList, remDataList) -> {
                    if (intDataList.isEmpty()) return remDataList;
                    var unionList = Collections.synchronizedList(new ArrayList<RemoteGameData>());
                    synchronized (unionList) {
                        intDataList.forEach(intData -> remDataList.forEach(remData -> {
                            if (!Objects.equals(intData.title, remData.title)) {
                                unionList.add(remData);
                            }
                        }));
                    }
                    return unionList;
                }, executor)
                .thenAcceptAsync(remDataList -> remDataList.forEach(remGameData -> gamesMap.put(remGameData.id, new GameData(remGameData))), executor)
                .thenApplyAsync(x -> {
                    var syncDataList = Collections.synchronizedCollection(gamesMap.values());
                    if (syncDataList.size() < 2) return syncDataList;
                    return syncDataList.stream()
                            .sorted(Comparator.comparing(game -> game.title.toLowerCase()))
                            .toList();
                }, executor)
                .thenAcceptAsync(list -> remoteDataList.postValue(list.stream().filter(d -> !isGameInstalled(d)).toList()), executor)
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION);
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
                        mapFiles.put(file.getName(), packedUri);
                    }
                }, executor)
                .thenRunAsync(() -> {
                    try {
                        objectToJson(listDirsFile, mapFiles);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, executor)
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.getMessage(), ErrorType.EXCEPTION);
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

    public void delEntryDirFromList(List<GameData> tempList, GameData data, File listDirsFile) {
        var gameDir = DocumentFileCompat.fromUri(getApplication(), data.gameDirUri);
        if (!isWritableDir(getApplication(), gameDir)) return;

        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, gameDir.getName()),
                        (unused, unused2) -> null,
                        executor
                )
                .thenRunAsync(() -> forceDelFile(getApplication(), gameDir), executor)
                .thenRunAsync(() -> dropPersistable(data.gameDirUri), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    public void delEntryFromList(List<GameData> tempList, GameData data, File listDirsFile) {
        var gameDir = DocumentFileCompat.fromUri(getApplication(), data.gameDirUri);
        if (!isWritableDir(getApplication(), gameDir)) return;

        CompletableFuture
                .runAsync(() -> tempList.remove(data), executor)
                .thenCombineAsync(
                        removeDirFromListDirsFile(listDirsFile, gameDir.getName()),
                        (unused, unused2) -> null,
                        executor
                )
                .thenRunAsync(() -> dropPersistable(data.gameDirUri), executor)
                .thenRun(this::refreshGameData)
                .exceptionally(ex -> {
                    doOnShowErrorDialog(ex.getMessage(), ErrorType.EXCEPTION);
                    return null;
                });
    }

    private CompletableFuture<Void> removeDirFromListDirsFile(File listDirsFile, String folderName) {
        var ref = new TypeReference<HashMap<String, String>>() {
        };

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return jsonToObject(listDirsFile, ref);
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, executor)
                .thenAcceptAsync(mapFiles -> {
                    if (!mapFiles.isEmpty()) {
                        mapFiles
                                .entrySet()
                                .removeIf(stringStringEntry -> stringStringEntry.getKey().equalsIgnoreCase(folderName));
                        try {
                            objectToJson(listDirsFile, mapFiles);
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }
                }, executor)
                .thenRunAsync(() -> {
                    var newList = extGamesListDir;
                    if (newList == null) return;

                    newList.removeIf(file -> {
                        var nameDir = file.getName();
                        return nameDir.equalsIgnoreCase(folderName);
                    });
                    extGamesListDir = newList;

                    ((QuestopiaApplication) getApplication()).setCurrentGameDir(null);
                    runOnUiThread(this::refreshGameData);
                })
                .exceptionally(throwable -> {
                    doOnShowErrorDialog(throwable.getMessage(), ErrorType.EXCEPTION);
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
                        doOnShowErrorDialog(exception.toString(), ErrorType.EXCEPTION);
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
                            .thenRun(() -> {
                                archive.delete();

                                var localRemoteGame = new LocalGame(getApplication());
                                var gameFolder = archiveUnpack.unpackFolder;
                                localRemoteGame.createDataIntoFolder(currGameData, gameFolder);
                            })
                            .thenRun(() -> {
                                var notificationBuild = new NotifyBuilder(getApplication(), UNPACK_GAME_CHANNEL_ID);
                                var unpackBody = ActivityCompat.getString(getApplication(), R.string.bodyUnpackDoneNotify);
                                var notification = notificationBuild.buildStandardNotification(
                                        ActivityCompat.getString(getApplication(), R.string.titleUnpackDoneNotify),
                                        unpackBody.replace("-GAMENAME-", currGameData.title)
                                );
                                var notificationManager = getApplication().getSystemService(NotificationManager.class);
                                notificationManager.notify(UNPACK_GAME_NOTIFICATION_ID, notification);
                            })
                            .exceptionally(throwable -> {
                                doOnShowErrorDialog(throwable.toString(), ErrorType.EXCEPTION);
                                return null;
                            });
                }
            }
        }
    }

}
