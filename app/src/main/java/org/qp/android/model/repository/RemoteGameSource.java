package org.qp.android.model.repository;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.qp.android.data.db.Game;
import org.qp.android.dto.stock.RemoteDataList;

import java.util.ArrayList;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RemoteGameSource extends RxPagingSource<Integer, Game> {

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
                    var mapper = new XmlMapper();
                    var string = body.string();
                    var matcher = Pattern.compile("max_pages=(\\d+)").matcher(string);
                    var newBody = matcher.find() ? string.replace(matcher.group(), "max_pages=\""+matcher.group(1)+"\"") : string;
                    return mapper.readValue(newBody, RemoteDataList.class);
                })
                .map(dataList -> toLoadResult(dataList, finalNextPageNumber));
    }

    private LoadResult<Integer, Game> toLoadResult(RemoteDataList dataList, int page) {
        var listRemoteGameEntry = new ArrayList<Game>();
        dataList.game.forEach(item -> {
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

        var maxPages = dataList.maxPages;
        return new LoadResult.Page<>(listRemoteGameEntry, page == 0 ? null : page - 1, page == maxPages ? null : page + 1);
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Game> pagingState) {
        return 0;
    }
}
