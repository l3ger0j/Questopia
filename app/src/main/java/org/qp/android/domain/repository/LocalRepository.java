package org.qp.android.domain.repository;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.qp.android.data.source.database.model.Game;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface LocalRepository {
    enum SortedMethod {
        ASCENDING,
        DESCENDING
    }

    CompletableFuture<Integer> deleteGamesEntries(List<Game> entriesDelete);
    CompletableFuture<Game> getGameEntryById(long entryId);
    CompletableFuture<List<Game>> getAllGameEntries();
    CompletableFuture<List<Game>> getAllSortGameEntries(SortedMethod method);
    CompletableFuture<Void> updateEntry(Game gameEntry);
    CompletableFuture<Void> updateOrInsertEntry(Game gameEntry);
    CompletableFuture<Void> insertDefaultEntry(Game newGameEntry);
    CompletableFuture<Void> fillAndInsertEntry(Game unfilledEntry, DocumentFile gameDir, List<Uri> gameFiles);
}
