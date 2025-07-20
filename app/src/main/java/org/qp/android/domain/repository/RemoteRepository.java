package org.qp.android.domain.repository;

import androidx.paging.rxjava3.RxPagingSource;

import org.qp.android.data.source.database.model.Game;

public interface RemoteRepository {
    RxPagingSource<Integer, Game> fetchRemoteSource();
}
