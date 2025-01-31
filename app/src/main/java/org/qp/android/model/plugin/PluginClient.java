package org.qp.android.model.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.questopiabundle.IQuestopiaBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PluginClient {

    private static final String ACTION_PICK_PLUGIN = "org.qp.intent.action.PICK_PLUGIN";
    private static final String ENGINE_PLUGIN_ID = "org.qp.android.plugin.ENGINE_PLUGIN";
    private static final String KEY_PKG = "pkg";
    private static final String KEY_SERVICENAME = "servicename";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_CATEGORIES = "categories";

    private final MutableLiveData<List<HashMap<String, String>>> servicesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> categoriesLiveData = new MutableLiveData<>();

    public IQuestopiaBundle questopiaBundle;

    private final ServiceConnection engineConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            questopiaBundle = IQuestopiaBundle.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questopiaBundle = null;
        }
    };

    private PluginClient() {}

    private static class PluginClientHolder {
        public static final PluginClient HOLDER_INSTANCE = new PluginClient();
    }

    public static PluginClient getInstance() {
        return PluginClientHolder.HOLDER_INSTANCE;
    }

    public LiveData<List<HashMap<String, String>>> getServicesLiveData() {
        return servicesLiveData;
    }

    public LiveData<List<String>> getCategoriesLiveData() {
        return categoriesLiveData;
    }

    public boolean isPluginExist(Context context, String serviceName) {
        loadListPlugin(context);
        if (!servicesLiveData.isInitialized()) return false;
        var currPluginList = servicesLiveData.getValue();
        if (currPluginList == null) return false;
        if (currPluginList.isEmpty()) return false;
        for (var element : currPluginList) {
            var service = element.get(KEY_SERVICENAME);
            if (service == null) return false;
            return service.equals(serviceName);
        }
        return false;
    }

    @Nullable
    public CompletableFuture<PluginInfo> requestInfo(Context context, PluginType pluginType) {
        return CompletableFuture
                .supplyAsync(() -> connectPlugin(context, pluginType))
                .thenApplyAsync(aBoolean -> {
                    if (!aBoolean) return null;
                    PluginInfo pluginInfo = null;

                    switch (pluginType) {
                        case ENGINE_PLUGIN -> {
                            try {
                                pluginInfo = new PluginInfo(
                                        questopiaBundle.versionPlugin(),
                                        questopiaBundle.titlePlugin(),
                                        questopiaBundle.authorPlugin()
                                );
                            } catch (RemoteException e) {
                                throw new CompletionException(e);
                            }
                        }
                    }

                    return pluginInfo;
                })
                .thenApplyAsync(info -> {
                    if (!disconnectPlugin(context, pluginType)) {
                        throw new CompletionException(new RuntimeException("Error disconnect plugin"));
                    }
                    return info;
                })
                .exceptionally(throwable -> {
                    // dosmt
                    return null;
                });
    }

    public boolean connectPlugin(Context context, PluginType pluginType) {
        switch (pluginType) {
            case ENGINE_PLUGIN -> {
                var intent = new Intent(ENGINE_PLUGIN_ID);
                var updatedIntent = createExplicitIntent(context, intent);
                if (updatedIntent != null) {
                    return context.bindService(updatedIntent, engineConn, Context.BIND_AUTO_CREATE);
                }
            }
        }
        return false;
    }

    public boolean disconnectPlugin(Context context, PluginType pluginType) {
        switch (pluginType) {
            case ENGINE_PLUGIN -> {
                var intent = new Intent(ENGINE_PLUGIN_ID);
                var updatedIntent = createExplicitIntent(context, intent);
                if (updatedIntent == null) return false;
                if (context.stopService(updatedIntent)) {
                    context.unbindService(engineConn);
                    return true;
                }
            }
        }
        return false;
    }

    public void loadListPlugin(Context context) {
        var servicesList = new ArrayList<HashMap<String, String>>();
        var categoriesList = new ArrayList<String>();
        var packageManager = context.getPackageManager();

        var baseIntent = new Intent(ACTION_PICK_PLUGIN);
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        var list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);

        for (int i = 0; i < list.size(); ++i) {
            var info = list.get(i);
            var serviceInfo = info.serviceInfo;
            var filter = info.filter;
            if (serviceInfo != null) {
                var item = new HashMap<String, String>();
                item.put(KEY_PKG, serviceInfo.packageName);
                item.put(KEY_SERVICENAME, serviceInfo.name);
                var firstCategory = "";

                if (filter != null) {
                    var actions = new StringBuilder();
                    for (var actionIterator = filter.actionsIterator(); actionIterator.hasNext();) {
                        var action = actionIterator.next();
                        if (actions.length() > 0) actions.append(",");
                        actions.append(action);
                    }

                    var categories = new StringBuilder();
                    for (var categoryIterator = filter.categoriesIterator(); categoryIterator.hasNext();) {
                        var category = categoryIterator.next();
                        if (firstCategory.isEmpty()) firstCategory = category;
                        if (categories.length() > 0) categories.append(",");
                        categories.append(category);
                    }

                    item.put(KEY_ACTIONS, new String(actions));
                    item.put(KEY_CATEGORIES, new String(categories));
                } else {
                    item.put(KEY_ACTIONS, "<null>");
                    item.put(KEY_CATEGORIES, "<null>");
                }

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
