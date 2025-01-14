package org.qp.android.helpers.utils;

import org.qp.android.data.db.Game;
import org.qp.android.data.db.GameDao;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class DatabaseUtil {

    private final GameDao gameDao;

    public enum SortedMethod {
        ASCENDING,
        DESCENDING
    }

    public DatabaseUtil(GameDao gameDao) {
        this.gameDao = gameDao;
    }

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
                        insertEntry(gameEntry);
                    }
                });
    }

    public CompletableFuture<Void> insertEntry(Game newGameEntry) {
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        gameDao.insert(newGameEntry);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });
    }

}
