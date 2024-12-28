package org.qp.android.model.repository;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RemoteService {

    @GET("/gamestock/gamestock_testing.php?per_page=10")
    Single<ResponseBody> pagingListRemoteGames(
            @Query("page") int numPage
    );

}
