package org.qp.android.model.repository;

import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.helpers.utils.DatabaseUtil;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalGame {

    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";
    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final GameDao gameDao;
    private final Context context;

    public LocalGame(GameDao gameDao , Context context) {
        this.gameDao = gameDao;
        this.context = context;
    }

    private void createNoMediaFile(@NonNull DocumentFile gameDir) {
        forceCreateFile(context , gameDir , MimeType.TEXT , NOMEDIA_FILENAME);
    }

    private void createNoSearchFile(@NonNull DocumentFile gameDir) {
        forceCreateFile(context , gameDir , MimeType.TEXT , NOSEARCH_FILENAME);
    }

    //        return CompletableFuture
//                .supplyAsync(() -> calculateDirSize(rootDir), executor)
//                .thenAccept(aLong -> emptyGameEntry.fileSize = String.valueOf(aLong));

    public CompletableFuture<Integer> deleteGamesEntries(List<Game> entriesDelete) {
        return CompletableFuture
                .supplyAsync(() -> gameDao.deleteList(entriesDelete), executor)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> createEntryInDBFromDir(File rootDir) {
        var nameDir = rootDir.getName();
        if (!isNotEmptyOrBlank(nameDir)) {
            var secureRandom = new SecureRandom();
            nameDir = "UnknownGame " + secureRandom.nextInt();
        }

        var gameFiles = new ArrayList<Uri>();
        var files = rootDir.listFiles();

        if (files == null) return new CompletableFuture<>();

        for (var file : files) {
            var dirName = file.getName();
            if (!isNotEmptyOrBlank(dirName)) continue;
            var locName = file.getName().toLowerCase(Locale.ROOT);
            if (locName.endsWith(".qsp") || locName.endsWith(".gam")) {
                gameFiles.add(Uri.fromFile(file));
            }
        }

        var emptyGameEntry = new Game();
        var databaseUtil = new DatabaseUtil(gameDao);

        emptyGameEntry.listId = 1;
        emptyGameEntry.title = nameDir;
        emptyGameEntry.gameDirUri = Uri.fromFile(rootDir);
        emptyGameEntry.gameFilesUri = gameFiles;

        return databaseUtil.insertEntry(emptyGameEntry);
    }

    public CompletableFuture<Void> createEntryInDBFromDir(DocumentFile rootDir) {
        var nameDir = rootDir.getName();
        if (!isNotEmptyOrBlank(nameDir)) {
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
        var databaseUtil = new DatabaseUtil(gameDao);

        emptyGameEntry.listId = 0;
        emptyGameEntry.title = nameDir;
        emptyGameEntry.gameDirUri = rootDir.getUri();
        emptyGameEntry.gameFilesUri = gameFiles;

        createNoMediaFile(rootDir);
        createNoSearchFile(rootDir);

        return databaseUtil.insertEntry(emptyGameEntry);
    }

    public CompletableFuture<Void> updateEntryInDB(Game gameEntry) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getById(gameEntry.id);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                } , executor)
                .thenAccept(game -> {
                    try {
                        if (game == null) return;
                        gameDao.update(gameEntry);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    return null;
                });
    }

    private record GameFolder(Uri gameUriDir , List<Uri> gameUriFiles) {}
}