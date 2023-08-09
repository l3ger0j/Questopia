package org.qp.android.model.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.qp.android.plugin.IQuestPlugin

class PluginClient {

    var namePlugin: String? = null
        private set
    val servicesLiveData = MutableLiveData<ArrayList<HashMap<String, String>>>()
    private val categoriesLiveData = MutableLiveData<ArrayList<String>>()
    var questPlugin: IQuestPlugin? = null
        private set
    var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            questPlugin = IQuestPlugin.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            questPlugin = null
        }
    }

    fun getServicesLiveData(): LiveData<ArrayList<HashMap<String, String>>> {
        return servicesLiveData
    }

    fun getCategoriesLiveData(): LiveData<ArrayList<String>> {
        return categoriesLiveData
    }

    fun connectPlugin(context: Context, pluginType: PluginType) {
        if (pluginType == PluginType.DOWNLOAD_PLUGIN) {
            val intent = Intent("org.qp.android.plugin.DOWNLOAD_PLUGIN")
            val updatedIntent = createExplicitIntent(context, intent)
            if (updatedIntent != null) {
                context.bindService(updatedIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun loadListPlugin(context: Context) {
        val servicesList = ArrayList<HashMap<String, String>>()
        val categoriesList = ArrayList<String>()
        val packageManager = context.packageManager
        val baseIntent = Intent(ACTION_PICK_PLUGIN)
        baseIntent.flags = Intent.FLAG_DEBUG_LOG_RESOLUTION
        val list: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                baseIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_RESOLVED_FILTER.toLong())
            )
        } else {
            packageManager.queryIntentActivities(
                baseIntent,
                PackageManager.GET_RESOLVED_FILTER
            )
        }

        for (i in list.indices) {
            val info = list[i]
            val serviceInfo = info.serviceInfo
            val filter = info.filter
            if (serviceInfo != null) {
                val item = HashMap<String, String>()
                item[KEY_PKG] = serviceInfo.packageName
                item[KEY_SERVICENAME] = serviceInfo.name
                namePlugin = serviceInfo.name
                var firstCategory: String? = null
                if (filter != null) {
                    val actions = StringBuilder()
                    val actionIterator = filter.actionsIterator()
                    while (actionIterator.hasNext()) {
                        val action = actionIterator.next()
                        if (actions.isNotEmpty()) actions.append(",")
                        actions.append(action)
                    }
                    val categories = StringBuilder()
                    val categoryIterator = filter.categoriesIterator()
                    while (categoryIterator.hasNext()) {
                        val category = categoryIterator.next()
                        if (firstCategory == null) firstCategory = category
                        if (categories.isNotEmpty()) categories.append(",")
                        categories.append(category)
                    }
                    item[KEY_ACTIONS] = actions.toString()
                    item[KEY_CATEGORIES] = categories.toString()
                } else {
                    item[KEY_ACTIONS] = "<null>"
                    item[KEY_CATEGORIES] = "<null>"
                }
                if (firstCategory == null) firstCategory = ""
                categoriesList.add(firstCategory)
                servicesList.add(item)
            }
        }

        servicesLiveData.postValue(servicesList)
        categoriesLiveData.postValue(categoriesList)
    }

    private fun createExplicitIntent(context: Context, intent: Intent): Intent? {
        val pm = context.packageManager
        val resolveInfo: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            pm.queryIntentActivities(
                intent,
                0
            )
        }
        if (resolveInfo.size != 1) {
            return null
        }

        val serviceInfo = resolveInfo[0]
        val packageName = serviceInfo.serviceInfo.packageName
        val className = serviceInfo.serviceInfo.name
        val component = ComponentName(packageName, className)
        val explicitIntent = Intent(intent)
        explicitIntent.component = component
        return explicitIntent
    }

    companion object {
        const val ACTION_PICK_PLUGIN = "org.qp.intent.action.PICK_PLUGIN"
        private const val KEY_PKG = "pkg"
        private const val KEY_SERVICENAME = "servicename"
        private const val KEY_ACTIONS = "actions"
        private const val KEY_CATEGORIES = "categories"
    }
}