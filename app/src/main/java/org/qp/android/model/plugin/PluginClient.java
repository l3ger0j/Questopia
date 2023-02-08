package org.qp.android.model.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import org.qp.android.IQuestPlugin;

public class PluginClient {
    private IQuestPlugin questPlugin;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name , IBinder service) {
            questPlugin = IQuestPlugin.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questPlugin = null;
        }
    };

    public void connectPlugin (Context context) {
        // var intent = new Intent(pluginName);
        // Log.d (getClass().getSimpleName(), intent.toString());
        // var updatedIntent = createExplicitIntent(context, intent);
        // if (updatedIntent != null) {
        //    context.bindService(updatedIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        // }
    }

    public int calculateSum(int firstNumber, int secondNumber) {
        int sum = -1;
        if (firstNumber != 0 && secondNumber != 0) {
            try {
                sum = questPlugin.sum(firstNumber, secondNumber);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return sum;
    }

    @Nullable
    private Intent createExplicitIntent(Context context, Intent intent) {
        var pm = context.getPackageManager();
        var resolveInfo = pm.queryIntentServices(intent , 0);
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
