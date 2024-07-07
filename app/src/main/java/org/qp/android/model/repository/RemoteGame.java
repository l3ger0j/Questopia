package org.qp.android.model.repository;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class RemoteGame {

    private final Retrofit client;

    public void getRemoteGameData(Callback<ResponseBody> callback) {
        client.create(RemoteService.class).listRemoteGames().enqueue(callback);
    }

    public RemoteGame() {
        client = new Retrofit.Builder()
                .baseUrl("https://qsp.org")
                .build();
    }

}
