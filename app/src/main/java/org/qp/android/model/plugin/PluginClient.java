package org.qp.android.model.plugin;

import static org.qp.android.helpers.utils.ThreadUtil.throwIfNotMainThread;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.questopiabundle.IQuestopiaBundle;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class PluginClient {

    public static final int LIB_DELAY = 3;
    private static final String TAG = PluginClient.class.getSimpleName();
    private static final String ENGINE_PLUGIN_ID = "org.qp.android.plugin.ENGINE_PLUGIN";
    private final MutableLiveData<List<PluginInfo>> infoPluginsLiveData = new MutableLiveData<>();
    private final ReentrantLock threadLock = new ReentrantLock();
    public IQuestopiaBundle questopiaBundle = new IQuestopiaBundle.Default();
    private final ServiceConnection engineConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            questopiaBundle = IQuestopiaBundle.Stub.asInterface(service);

            try {
                var pluginInfo = new PluginInfo("", "", "");
                pluginInfo = new PluginInfo(
                        questopiaBundle.versionPlugin(),
                        questopiaBundle.titlePlugin(),
                        questopiaBundle.authorPlugin()
                );
                infoPluginsLiveData.postValue(List.of(pluginInfo));
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questopiaBundle = null;
        }
    };
    private Thread pluginClientThread;
    private volatile Handler threadHandler;
    private volatile boolean threadInit;
    private volatile boolean isInitPlugin = false;

    private static class PluginClientHolder {
        public static final PluginClient HOLDER_INSTANCE = new PluginClient();
    }

    private PluginClient() {
    }

    public static PluginClient getInstance() {
        return PluginClientHolder.HOLDER_INSTANCE;
    }

    public LiveData<List<PluginInfo>> getInfoPluginsLiveData() {
        return infoPluginsLiveData;
    }

    public void startThread() {
        pluginClientThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    threadHandler = new Handler(Looper.myLooper());
                    threadInit = true;
                    Looper.loop();
                } catch (Throwable t) {
                    Log.e(TAG, "thread has stopped exceptionally", t);
                    Thread.currentThread().interrupt();
                }
            }
        }, "pluginClientThread");
        pluginClientThread.start();
    }

    public void stopThread() {
        throwIfNotMainThread();
        if (pluginClientThread == null) return;
        if (threadInit) {
            var handler = threadHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            threadInit = false;
        } else {
            Log.w(TAG, "thread has been started, but not initialized");
        }
        pluginClientThread.interrupt();
    }

    public void runOnThread(final Runnable runnable) {
        throwIfNotMainThread();
        if (pluginClientThread == null) {
            Log.w(TAG, "thread has not been started");
            return;
        }
        if (!threadInit) {
            Log.w(TAG, "thread has been started, but not initialized");
            return;
        }

        var mLibHandler = threadHandler;
        if (mLibHandler == null) return;
        mLibHandler.post(() -> {
            threadLock.lock();
            try {
                runnable.run();
            } finally {
                threadLock.unlock();
            }
        });
    }

    public void connectAllPlugin(Context context) {
        for (var pluginType : PluginType.values()) {
            switch (pluginType) {
                case ENGINE_PLUGIN -> runOnThread(() -> {
                    var intent = new Intent(ENGINE_PLUGIN_ID);
                    var updatedIntent = createExplicitIntent(context, intent);
                    if (updatedIntent == null) return;
                    isInitPlugin = context.bindService(updatedIntent, engineConn, Context.BIND_AUTO_CREATE);
                });
            }
        }
    }

    public void connectPlugin(Context context, PluginType pluginType) {
        switch (pluginType) {
            case ENGINE_PLUGIN -> runOnThread(() -> {
                var intent = new Intent(ENGINE_PLUGIN_ID);
                var updatedIntent = createExplicitIntent(context, intent);
                if (updatedIntent == null) return;
                isInitPlugin = context.bindService(updatedIntent, engineConn, Context.BIND_AUTO_CREATE);
            });
        }
    }

    public void disconnectAllPlugin(Context context) {
        if (!isInitPlugin) return;
        for (var pluginType : PluginType.values()) {
            switch (pluginType) {
                case ENGINE_PLUGIN -> runOnThread(() -> {
                    var intent = new Intent(ENGINE_PLUGIN_ID);
                    var updatedIntent = createExplicitIntent(context, intent);
                    if (updatedIntent == null) return;
                    context.stopService(updatedIntent);
                });
            }
        }
    }

    public void disconnectPlugin(Context context, PluginType pluginType) {
        switch (pluginType) {
            case ENGINE_PLUGIN -> runOnThread(() -> {
                var intent = new Intent(ENGINE_PLUGIN_ID);
                var updatedIntent = createExplicitIntent(context, intent);
                if (updatedIntent == null) return;
                context.stopService(updatedIntent);
            });
        }
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
        var explicitIntent = new Intent(intent);
        explicitIntent.setComponent(component);

        return explicitIntent;
    }
}
