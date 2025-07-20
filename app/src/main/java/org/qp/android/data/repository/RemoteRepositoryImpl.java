package org.qp.android.data.repository;

import androidx.annotation.NonNull;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.qp.android.data.mapper.ConvertList;
import org.qp.android.data.source.database.model.Game;
import org.qp.android.data.source.network.RemoteGame;
import org.qp.android.data.source.network.model.RemoteDataList;
import org.qp.android.domain.repository.RemoteRepository;

import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class RemoteRepositoryImpl implements RemoteRepository {
    @Override
    public RxPagingSource<Integer, Game> fetchRemoteSource() {
        return new RxPagingSource<>() {
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
                            var newBody = matcher.find() ? string.replace(matcher.group(), "max_pages=\"" + matcher.group(1) + "\"") : string;
                            return mapper.readValue(newBody, RemoteDataList.class);
                        })
                        .map(dataList -> toLoadResult(dataList, finalNextPageNumber));
            }

            private LoadResult<Integer, Game> toLoadResult(RemoteDataList dataList, int page) {
                final var maxPages = dataList.maxPages;
                final var listRemoteGameEntry = ConvertList.remoteDataListToEntryList(dataList.game);
                return new LoadResult.Page<>(listRemoteGameEntry, page == 0 ? null : page - 1, page == maxPages ? null : page + 1);
            }

            @NonNull
            @Override
            public Integer getRefreshKey(@NonNull PagingState<Integer, Game> pagingState) {
                return 0;
            }
        };
    }
}
