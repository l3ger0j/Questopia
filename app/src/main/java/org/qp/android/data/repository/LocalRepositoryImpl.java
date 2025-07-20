package org.qp.android.data.repository;

import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.FileUtil.tryReceiveDirSize;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.qp.android.data.source.database.GameDao;
import org.qp.android.data.source.database.model.Game;
import org.qp.android.domain.repository.LocalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public final class LocalRepositoryImpl implements LocalRepository {

    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";
    private final String TAG = this.getClass().getSimpleName();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final GameDao gameDao;
    private final Context context;

    @Inject
    public LocalRepositoryImpl(@NonNull Context context,
                               @NonNull GameDao gameDao) {
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
    @Override
    public CompletableFuture<Integer> deleteGamesEntries(List<Game> entriesDelete) {
        var newEntriesList = new ArrayList<>(entriesDelete);
        return CompletableFuture
                .supplyAsync(() -> gameDao.deleteList(newEntriesList), executor)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error: ", throwable);
                    return null;
                });
    }

    @Override
    public CompletableFuture<Game> getGameEntryById(long entryId) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getById(entryId);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<List<Game>> getAllGameEntries() {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getAll();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<List<Game>> getAllSortGameEntries(SortedMethod method) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getAllSortedByName(method.ordinal());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> updateEntry(Game gameEntry) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getById(gameEntry.id);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(game -> {
                    if (game == null) return;
                    try {
                        gameDao.update(gameEntry);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> updateOrInsertEntry(Game gameEntry) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return gameDao.getById(gameEntry.id);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .thenAccept(game -> {
                    if (game != null) {
                        try {
                            gameDao.update(game);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    } else {
                        insertDefaultEntry(gameEntry);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> insertDefaultEntry(Game newGameEntry) {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        gameDao.insert(newGameEntry);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> fillAndInsertEntry(Game unfilledEntry,
                                                      DocumentFile gameDir,
                                                      List<Uri> gameFiles) {
        unfilledEntry.listId = 0;
        unfilledEntry.gameDirUri = gameDir.getUri();
        unfilledEntry.gameFilesUri = gameFiles;

        var gameDirSize = tryReceiveDirSize(context.getContentResolver(), gameDir.getUri());
        if (gameDirSize != 0) {
            unfilledEntry.fileSize = gameDirSize;
        }

        CompletableFuture.runAsync(() -> {
            createNoMediaFile(gameDir);
            createNoSearchFile(gameDir);
        });

        return this.insertDefaultEntry(unfilledEntry);
    }
}
