package org.qp.android.model.repository;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RemoteService {

    @GET("gamestock/gamestock2.php")
    Call<ResponseBody> listRemoteGames();

}
