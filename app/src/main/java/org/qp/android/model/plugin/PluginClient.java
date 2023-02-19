package org.qp.android.model.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.plugin.IQuestPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public class PluginClient {

    public static final String ACTION_PICK_PLUGIN = "org.qp.intent.action.PICK_PLUGIN";
    private static final String KEY_PKG = "pkg";
    private static final String KEY_SERVICENAME = "servicename";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_CATEGORIES = "categories";

    private String namePlugin;
    private final MutableLiveData<ArrayList<HashMap<String, String>>> servicesLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<ArrayList<String>> categoriesLiveData =
            new MutableLiveData<>();
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

    public IQuestPlugin getQuestPlugin() {
        return questPlugin;
    }

    public String getNamePlugin() {
        return namePlugin;
    }

    public LiveData<ArrayList<HashMap<String, String>>> getServicesLiveData() {
        return servicesLiveData;
    }

    public LiveData<ArrayList<String>> getCategoriesLiveData() {
        return categoriesLiveData;
    }

    public void connectPlugin (Context context, PluginType pluginType) {
        if (pluginType == PluginType.DOWNLOAD_PLUGIN) {
            var intent = new Intent("org.qp.android.plugin.DOWNLOAD_PLUGIN");
            var updatedIntent = createExplicitIntent(context, intent);
            if (updatedIntent != null) {
                context.bindService(updatedIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    public void loadListPlugin(Context context) {
        ArrayList<HashMap<String, String>> servicesList = new ArrayList<>();
        ArrayList<String> categoriesList = new ArrayList<>();
        var packageManager = context.getPackageManager();
        var baseIntent = new Intent(ACTION_PICK_PLUGIN);
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        var list = packageManager.queryIntentServices(baseIntent,
                PackageManager.GET_RESOLVED_FILTER);
        for (int i = 0; i < list.size(); ++i) {
            var info = list.get(i);
            var serviceInfo = info.serviceInfo;
            var filter = info.filter;
            if (serviceInfo != null) {
                var item = new HashMap<String, String>();
                item.put(KEY_PKG, serviceInfo.packageName);
                item.put(KEY_SERVICENAME, serviceInfo.name);
                namePlugin = serviceInfo.name;
                String firstCategory = null;
                if (filter != null) {
                    var actions = new StringBuilder();
                    for (var actionIterator = filter.actionsIterator();
                         actionIterator.hasNext(); ) {
                        var action = actionIterator.next();
                        if (actions.length() > 0)
                            actions.append(",");
                        actions.append(action);
                    }
                    var categories = new StringBuilder();
                    for (var categoryIterator = filter.categoriesIterator();
                         categoryIterator.hasNext(); ) {
                        var category = categoryIterator.next();
                        if (firstCategory == null)
                            firstCategory = category;
                        if (categories.length() > 0)
                            categories.append(",");
                        categories.append(category);
                    }
                    item.put(KEY_ACTIONS, new String(actions));
                    item.put(KEY_CATEGORIES, new String(categories));
                } else {
                    item.put(KEY_ACTIONS, "<null>");
                    item.put(KEY_CATEGORIES, "<null>");
                }
                if (firstCategory == null)
                    firstCategory = "";
                categoriesList.add(firstCategory);
                servicesList.add(item);
            }
        }
        servicesLiveData.postValue(servicesList);
        categoriesLiveData.postValue(categoriesList);
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
