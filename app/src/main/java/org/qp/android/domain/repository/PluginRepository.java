package org.qp.android.domain.repository;

import android.content.Context;
import android.content.ServiceConnection;

import java.util.concurrent.CompletableFuture;

public interface PluginRepository {
    boolean isPluginExist(Context context);
    CompletableFuture<Boolean> connectEnginePlugin(Context context, ServiceConnection connection);
    CompletableFuture<Boolean> disconnectEnginePlugin(Context context, Runnable runnable);
}
