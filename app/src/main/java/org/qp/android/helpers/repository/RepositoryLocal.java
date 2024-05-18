package org.qp.android.helpers.repository;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;
import static org.qp.android.helpers.utils.FileUtil.createFindDFile;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.FileUtil.readFileAsString;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;

import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.dto.stock.GameData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositoryLocal {

    private final String TAG = this.getClass().getSimpleName();

    private static final String GAME_INFO_FILENAME = ".gameInfo";
    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";

    private final ExecutorService executor = Executors.newWorkStealingPool();

    private final GameDao gameDao;
    private final Context context;

    @NonNull
    private List<GameFolder> getGamesFolders(@NonNull List<DocumentFile> dirs) {
        var folders = new ArrayList<GameFolder>();

        for (var dir : dirs) {
            var gameFiles = new ArrayList<DocumentFile>();
            var files = dir.listFiles();

            for (var file : files) {
                if (file.getName() == null) continue;
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }

            folders.add(new GameFolder(dir , gameFiles));
        }

        return folders;
    }

    @Nullable
    private DocumentFile getGameInfoFile(@NonNull DocumentFile gameFolder) {
        var findGameInfoFile = gameFolder.findFile(GAME_INFO_FILENAME);
        if (findGameInfoFile == null) {
            Log.w(TAG , "GameData info file not found in " + gameFolder.getName());
            return null;
        }
        return findGameInfoFile;
    }

    public RepositoryLocal(GameDao gameDao, Context context) {
        this.gameDao = gameDao;
        this.context = context;
    }

    private void createNoMediaFile(@NonNull DocumentFile gameDir) {
        var findNoMediaFile = gameDir.findFile(NOMEDIA_FILENAME);
        if (findNoMediaFile == null || !findNoMediaFile.exists()) {
            forceCreateFile(gameDir , MimeType.TEXT , NOMEDIA_FILENAME);
        }
    }

    private void createNoSearchFile(@NonNull DocumentFile gameDir) {
        var findNoSearchFile = gameDir.findFile(NOSEARCH_FILENAME);
        if (findNoSearchFile == null || !findNoSearchFile.exists()) {
            forceCreateFile(gameDir , MimeType.TEXT , NOSEARCH_FILENAME);
        }
    }

    public void createDataIntoFolder(Context context ,
                                     GameData gameData ,
                                     DocumentFile gameDir) {
        var infoFile = getGameInfoFile(gameDir);
        if (infoFile == null) {
            infoFile = createFindDFile(gameDir , MimeType.TEXT , GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            Log.e(TAG , "ERROR");
            return;
        }
        var tempInfoFile = documentWrap(infoFile);

        try (var out = tempInfoFile.openOutputStream(context , false)) {
            objectToJson(out , gameData);
        } catch (Exception ex) {
            Log.e(TAG , "ERROR: " , ex);
        }
    }

    public CompletableFuture<List<GameData>> getGameDataFromDB() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getAllSortedByName(1);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor)
                .thenApply(games -> {
                    var listGameData = new ArrayList<GameData>();
                    for (var game : games) {
                        var emptyGameData = new GameData();

                        emptyGameData.id = game.id;
                        emptyGameData.author = game.author;
                        emptyGameData.portedBy = game.portedBy;
                        emptyGameData.version = game.version;
                        emptyGameData.title = game.title;
                        emptyGameData.lang = game.lang;
                        emptyGameData.player = game.player;
                        emptyGameData.icon = game.icon;
                        emptyGameData.fileUrl = game.fileUrl;
                        emptyGameData.fileSize = game.fileSize;
                        emptyGameData.fileExt = game.fileExt;
                        emptyGameData.descUrl = game.descUrl;
                        emptyGameData.pubDate = game.pubDate;
                        emptyGameData.modDate = game.modDate;
                        emptyGameData.gameDir = DocumentFileCompat.fromUri(context, game.gameDirUri);

                        var gameDocList = new ArrayList<DocumentFile>();
                        game.gameFilesUri.forEach(uri ->
                                gameDocList.add(DocumentFileCompat.fromUri(context, uri)));
                        emptyGameData.gameFiles = gameDocList;

                        listGameData.add(emptyGameData);
                    }
                    return listGameData;
                });
    }

    public CompletableFuture<Void> createGameEntryInDB(DocumentFile rootDir) {
        var nameDir = rootDir.getName();
        if (nameDir == null) {
            var secureRandom = new SecureRandom();
            nameDir = "UnknownGame " + secureRandom.nextInt();
        }

        var gameFiles = new ArrayList<Uri>();
        var files = rootDir.listFiles();

        for (var file : files) {
            if (file.getName() == null) continue;
            var lcName = file.getName().toLowerCase(Locale.ROOT);
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                gameFiles.add(file.getUri());
            }
        }

        var emptyGameEntry = new Game();

        emptyGameEntry.id = nameDir.toLowerCase(Locale.ROOT);
        emptyGameEntry.title = nameDir;
        emptyGameEntry.gameDirUri = rootDir.getUri();
        emptyGameEntry.gameFilesUri = gameFiles;

        createNoMediaFile(rootDir);
        createNoSearchFile(rootDir);

        return CompletableFuture
                .supplyAsync(() -> calculateDirSize(rootDir), executor)
                .thenAccept(aLong -> emptyGameEntry.fileSize = String.valueOf(aLong))
                .thenApply(unused -> gameDao.getByName(emptyGameEntry.title))
                .thenAccept(game -> {
                    if (game != null) {
                        if (game.gameDirUri.equals(emptyGameEntry.gameDirUri)) return;
                        var secureRandom = new SecureRandom();
                        emptyGameEntry.id = game.id + secureRandom.nextInt();
                        emptyGameEntry.title = game.title+"(1)";
                    }
                    gameDao.insert(emptyGameEntry);
                });
    }

    public CompletableFuture<Void> updateGameEntryInDB(GameData data) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getByName(data.id);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor)
                .thenApply(game -> {
                    game.author = data.author;
                    game.portedBy = data.portedBy;
                    game.version = data.version;
                    game.title = data.title;
                    game.lang = data.lang;
                    game.player = data.player;
                    game.icon = data.icon;
                    game.fileUrl = data.fileUrl;
                    game.fileExt = data.fileExt;
                    game.descUrl = data.descUrl;
                    game.pubDate = data.pubDate;
                    game.modDate = data.modDate;
                    return game;
                })
                .thenAccept(game -> {
                    try {
                        gameDao.update(game);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: ", throwable);
                    return null;
                });
    }

    public List<GameData> extractGameDataFromList(Context context , List<DocumentFile> fileList) {
        if (fileList.isEmpty()) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = new ArrayList<GameData>();
        var formatGamesDirs = getGamesFolders(fileList);

        for (var gameFolder : formatGamesDirs) {
            var item = (GameData) null;
            var infoFile = getGameInfoFile(gameFolder.dir);

            if (infoFile == null) {
                var name = gameFolder.dir.getName();
                if (name == null) return Collections.emptyList();
                item = new GameData();
                item.id = name;
                item.title = name;
                item.gameDir = gameFolder.dir;
                item.gameFiles = gameFolder.gameFiles;
                createDataIntoFolder(context , item , gameFolder.dir);

                itemsGamesDirs.add(item);
            } else {
                var infoFileCont = readFileAsString(context , infoFile.getUri());

                if (infoFileCont != null) {
                    item = parseGameInfo(infoFileCont);
                }

                if (item == null) {
                    var name = gameFolder.dir.getName();
                    if (name == null) return Collections.emptyList();
                    item = new GameData();
                    item.id = name;
                    item.title = name;
                    item.gameDir = gameFolder.dir;
                    item.gameFiles = gameFolder.gameFiles;
                    createDataIntoFolder(context , item , gameFolder.dir);
                    itemsGamesDirs.add(item);
                } else {
                    item.gameDir = gameFolder.dir;
                    item.gameFiles = gameFolder.gameFiles;
                    itemsGamesDirs.add(item);
                }
            }

            createNoMediaFile(gameFolder.dir);
            createNoSearchFile(gameFolder.dir);
        }

        return itemsGamesDirs;
    }

    @Nullable
    private GameData parseGameInfo(String json) {
        try {
            return jsonToObject(json , GameData.class);
        } catch (Exception ex) {
            Log.e(TAG , "Failed to parse game info file" , ex);
            return null;
        }
    }

    private record GameFolder(DocumentFile dir , List<DocumentFile> gameFiles) {}
}