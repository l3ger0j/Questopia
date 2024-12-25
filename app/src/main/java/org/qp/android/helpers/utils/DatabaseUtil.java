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

    public void updateOrInsertEntry(Game gameEntry) {
        CompletableFuture
                .supplyAsync(() -> gameDao.getById(gameEntry.id))
                .thenAccept(game -> {
                    if (game != null) {
                        gameDao.update(game);
                    } else {
                        gameDao.insert(gameEntry);
                    }
                });
    }

    public CompletableFuture<Void> insertEntry(Game newGameEntry) {
        return CompletableFuture
                .supplyAsync(() -> gameDao.getById(newGameEntry.id))
                .thenAcceptAsync(gameEntry -> {
                    if (gameEntry != null) {
//                        if (gameEntry.gameDirUri.equals(newGameEntry.gameDirUri)) return;
//                        var secureRandom = new SecureRandom();
//                        newGameEntry.id = gameEntry.id + secureRandom.nextInt();
//                        newGameEntry.title = gameEntry.title+"(1)";
                    } else {
                        gameDao.insert(newGameEntry);
                    }
                });
    }

}
