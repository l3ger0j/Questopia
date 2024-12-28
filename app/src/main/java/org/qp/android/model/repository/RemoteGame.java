package org.qp.android.model.repository;

import androidx.annotation.NonNull;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;

public class RemoteGame {

    @NonNull
    public RemoteService getRemoteGameEntry() {
        var retrofit = new Retrofit.Builder()
                .baseUrl("https://qsp.org")
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        return retrofit.create(RemoteService.class);
    }

}
