package org.qp.android.data.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import androidx.annotation.Nullable;

import org.qp.android.domain.repository.PluginRepository;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PluginRepositoryImpl implements PluginRepository {

    private static final String ENGINE_PLUGIN_ID = "org.qp.android.plugin.ENGINE_PLUGIN";
    private final ExecutorService clientExecutor;

    public PluginRepositoryImpl() {
        this.clientExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean isPluginExist(Context context) {
        var intent = new Intent(ENGINE_PLUGIN_ID);
        var updatedIntent = createExplicitIntent(context, intent);
        return updatedIntent != null;
    }

    @Override
    public CompletableFuture<Boolean> connectEnginePlugin(Context context, ServiceConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            var intent = new Intent(ENGINE_PLUGIN_ID);
            var updatedIntent = createExplicitIntent(context, intent);
            if (updatedIntent == null) return false;
            return context.bindService(updatedIntent, connection, Context.BIND_AUTO_CREATE);
        }, clientExecutor);
    }

    @Override
    public CompletableFuture<Boolean> disconnectEnginePlugin(Context context, Runnable runnable) {
        return CompletableFuture
                .runAsync(runnable)
                .thenCombine(CompletableFuture.supplyAsync(() -> {
                    var intent = new Intent(ENGINE_PLUGIN_ID);
                    var updatedIntent = createExplicitIntent(context, intent);
                    if (updatedIntent == null) return false;
                    return context.stopService(updatedIntent);
                }, clientExecutor), (Void, aBool) -> {
                    if (!aBool) {
                        throw new CompletionException(new Exception("Error disconnect plugin!"));
                    } else {
                        return true;
                    }
                });
    }

    @Nullable
    private Intent createExplicitIntent(Context context, Intent intent) {
        var pm = context.getPackageManager();
        var resolveInfo = pm.queryIntentServices(intent, 0);

        if (resolveInfo.isEmpty()) {
            return null;
        }

        var serviceInfo = resolveInfo.get(0);
        var packageName = serviceInfo.serviceInfo.packageName;
        var className = serviceInfo.serviceInfo.name;
        var component = new ComponentName(packageName, className);

        return new Intent(intent).setComponent(component);
    }
}
