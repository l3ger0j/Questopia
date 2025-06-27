package org.qp.android.model.repository;

import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.FileUtil.tryReceiveDirSize;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;
import org.qp.android.helpers.utils.DatabaseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        forceCreateFile(context, gameDir, NOMEDIA_FILENAME, MimeType.TEXT);
    }

    private void createNoSearchFile(@NonNull DocumentFile gameDir) {
        forceCreateFile(context, gameDir, NOSEARCH_FILENAME, MimeType.TEXT);
    }

    //        return CompletableFuture
//                .supplyAsync(() -> calculateDirSize(rootDir), executor)
//                .thenAccept(aLong -> emptyGameEntry.fileSize = String.valueOf(aLong));

    public CompletableFuture<Integer> deleteGamesEntries(List<Game> entriesDelete) {
        var newEntriesList = new ArrayList<>(entriesDelete);
        return CompletableFuture
                .supplyAsync(() -> gameDao.deleteList(newEntriesList), executor)
                .exceptionally(throwable -> {
                    Log.e(TAG , "Error: " , throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> insertEntryInDB(Game unfilledEntry,
                                                   DocumentFile gameDir,
                                                   List<Uri> gameFiles) {
        unfilledEntry.listId = 0;
        unfilledEntry.gameDirUri = gameDir.getUri();
        unfilledEntry.gameFilesUri = gameFiles;

        var gameDirSize = tryReceiveDirSize(context.getContentResolver(), gameDir.getUri());
        if (gameDirSize != 0) {
            unfilledEntry.fileSize = gameDirSize;
        }

        createNoMediaFile(gameDir);
        createNoSearchFile(gameDir);

        var databaseUtil = new DatabaseUtil(gameDao);
        return databaseUtil.insertEntry(unfilledEntry);
    }

    public CompletableFuture<Void> updateEntryInDB(Game gameEntry) {
        var databaseUtil = new DatabaseUtil(gameDao);
        return databaseUtil.updateEntry(gameEntry);
    }
}