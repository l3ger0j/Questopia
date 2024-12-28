package org.qp.android.model.repository;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.qp.android.data.db.Game;
import org.qp.android.dto.stock.RemoteDataList;
import org.qp.android.dto.stock.RemoteGameData;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RemoteGamePagingSource extends RxPagingSource<Integer, Game> {

    @NonNull
    @Override
    public Single<LoadResult<Integer, Game>> loadSingle(@NonNull LoadParams<Integer> loadParams) {
        var nextPageNumber = loadParams.getKey();
        if (nextPageNumber == null) {
            nextPageNumber = 0;
        }

        var service = new RemoteGame();
        var finalNextPageNumber = nextPageNumber;
        return service.getRemoteGameEntry()
                .pagingListRemoteGames(nextPageNumber)
                .subscribeOn(Schedulers.io())
                .map(body -> {
                    Log.d(this.getClass().getSimpleName(), body.string());
                    var mapper = new XmlMapper();
                    var string = body.string();
                    var value = mapper.readValue(string, RemoteDataList.class);
                    return value.game;
                })
                .map(list -> toLoadResult(list, finalNextPageNumber));
    }

    private LoadResult<Integer, Game> toLoadResult(List<RemoteGameData> remoteData, int page) {
        var listRemoteGameEntry = new ArrayList<Game>();
        remoteData.forEach(item -> {
            var emptyEntry = new Game();
            emptyEntry.listId = 1;
            emptyEntry.author = item.author;
            emptyEntry.portedBy = item.portedBy;
            emptyEntry.version = item.version;
            emptyEntry.title = item.title;
            emptyEntry.lang = item.lang;
            emptyEntry.player = item.player;
            emptyEntry.gameIconUri = Uri.parse(item.icon);
            emptyEntry.fileUrl = item.fileUrl;
            emptyEntry.fileSize = item.fileSize;
            emptyEntry.fileExt = item.fileExt;
            emptyEntry.descUrl = item.descUrl;
            emptyEntry.pubDate = item.pubDate;
            emptyEntry.modDate = item.modDate;
            listRemoteGameEntry.add(emptyEntry);
        });

        return new LoadResult.Page<>(listRemoteGameEntry, page == 0 ? null : page - 1, page + 1);
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Game> pagingState) {
        return 0;
    }
}
