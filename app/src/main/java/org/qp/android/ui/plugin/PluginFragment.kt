package org.qp.android.ui.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.qp.android.R
import org.qp.android.databinding.FragmentPluginBinding
import org.qp.android.dto.plugin.PluginInfo
import org.qp.android.helpers.adapters.RecyclerItemClickListener
import org.qp.android.model.plugin.PluginClient
import org.qp.android.model.plugin.PluginType
import org.qp.android.plugin.IQuestPlugin
import org.qp.android.ui.settings.SettingsViewModel

class PluginFragment : Fragment() {

    private val TAG = javaClass.simpleName
    private var recyclerView: RecyclerView? = null
    private var services: ArrayList<HashMap<String, String>>? = null
    private var packageBroadcastReceiver: PackageBroadcastReceiver? = null
    private var packageFilter: IntentFilter? = null
    private var namePlugin: String? = null
    private var questPlugin: IQuestPlugin? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().setTitle(R.string.pluginMenuTitle)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController(requireView()).navigate(R.id.settingsFragment)
            }
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
        packageBroadcastReceiver = PackageBroadcastReceiver()
        packageFilter = IntentFilter()
        val fragmentPluginBinding = FragmentPluginBinding.inflate(
            layoutInflater
        )
        recyclerView = fragmentPluginBinding.pluginRecyclerView
        val settingsViewModel = ViewModelProvider(requireActivity())[SettingsViewModel::class.java]
        fragmentPluginBinding.pluginViewModel = settingsViewModel
        settingsViewModel.fragmentObservableField.set(this)
        refreshPluginInfo()
        return fragmentPluginBinding.root
    }

    private fun refreshPluginInfo() {
        val pluginInfo = PluginInfo()
        val pluginClient = PluginClient()
        fillPluginList(pluginClient)
        if (namePlugin != null) {
            pluginClient.connectPlugin(requireContext(), PluginType.DOWNLOAD_PLUGIN)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    questPlugin = pluginClient.questPlugin
                    pluginInfo.title = questPlugin?.titlePlugin()
                    pluginInfo.version = questPlugin?.versionPlugin()
                    pluginInfo.author = questPlugin?.authorPlugin()
                    val arrayList = ArrayList<PluginInfo>()
                    arrayList.add(pluginInfo)
                    val adapter = PluginRecycler(requireActivity())
                    adapter.submitList(arrayList)
                    recyclerView!!.adapter = adapter
                } catch (e: RemoteException) {
                    Log.d(TAG , "Error: " , e)
                }
            }, 1000)
        }
        packageFilter!!.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageFilter!!.addAction(Intent.ACTION_PACKAGE_REPLACED)
        packageFilter!!.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageFilter!!.addCategory(Intent.CATEGORY_DEFAULT)
        packageFilter!!.addDataScheme("package")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView!!.addOnItemTouchListener(
            RecyclerItemClickListener(
                context,
                recyclerView!!,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        val launchIntent = requireActivity().packageManager
                            .getLaunchIntentForPackage("org.qp.android.plugin")
                        launchIntent?.let { startActivity(it) }
                    }

                    override fun onLongItemClick(view: View?, position: Int) {}
                })
        )
    }

    private fun fillPluginList(pluginClient: PluginClient) {
        pluginClient.loadListPlugin(requireContext())
        namePlugin = pluginClient.namePlugin
        pluginClient.servicesLiveData.observe(
            viewLifecycleOwner
        ) { hashMaps: ArrayList<HashMap<String, String>>? -> services = hashMaps }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(packageBroadcastReceiver, packageFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(packageBroadcastReceiver)
    }

    internal inner class PackageBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: $intent")
            services!!.clear()
            fillPluginList(PluginClient())
        }
    }
}