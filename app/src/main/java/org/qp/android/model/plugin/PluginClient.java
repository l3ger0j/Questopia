package org.qp.android.model.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.qp.android.IQuestPlugin;

public class PluginClient {
    private IQuestPlugin servicePlugin;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name , IBinder service) {
            servicePlugin = IQuestPlugin.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicePlugin = null;
        }
    };

    public void connectPlugin (Context context, String pluginName) {
        var intent = new Intent(pluginName);
        Log.d (getClass().getSimpleName(), intent.toString());
        var updatedIntent = createExplicitIntent(context, intent);
        if (updatedIntent != null) {
            context.bindService(updatedIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private Intent createExplicitIntent(Context context, Intent intent) {
        var pm = context.getPackageManager();
        var resolveInfo = pm.queryIntentServices(intent, 0);
        if (resolveInfo == null || resolveInfo.size() != 1) {
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
