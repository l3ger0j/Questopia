package org.qp.android.ui.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginType;
import org.qp.android.plugin.IQuestPlugin;
import org.qp.android.ui.settings.SettingsViewModel;

import java.util.ArrayList;
import java.util.HashMap;

public class PluginFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();

    private RecyclerView recyclerView;
    private ArrayList<HashMap<String, String>> services;

    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;
    private String namePlugin;
    private  IQuestPlugin questPlugin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.pluginMenuTitle);

        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.settingsFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);

        packageBroadcastReceiver = new PackageBroadcastReceiver();
        packageFilter = new IntentFilter();
        var fragmentPluginBinding =
                FragmentPluginBinding.inflate(getLayoutInflater());
        recyclerView = fragmentPluginBinding.pluginRecyclerView;
        var settingsViewModel = new ViewModelProvider(requireActivity())
                .get(SettingsViewModel.class);
        fragmentPluginBinding.setPluginViewModel(settingsViewModel);
        settingsViewModel.fragmentObservableField.set(this);
        refreshPluginInfo();
        return fragmentPluginBinding.getRoot();
    }

    public void refreshPluginInfo() {
        var pluginInfo = new PluginInfo();
        var pluginClient = new PluginClient();

        fillPluginList(pluginClient);

        if (namePlugin != null) {
            pluginClient.connectPlugin(requireContext() , PluginType.DOWNLOAD_PLUGIN);
            new Handler().postDelayed(() -> {
                try {
                    questPlugin = pluginClient.getQuestPlugin();
                    pluginInfo.title = questPlugin.titlePlugin();
                    pluginInfo.version = questPlugin.versionPlugin();
                    pluginInfo.author = questPlugin.authorPlugin();
                    var arrayList = new ArrayList<PluginInfo>();
                    arrayList.add(pluginInfo);
                    var adapter =
                            new PluginRecycler(requireActivity());
                    adapter.submitList(arrayList);
                    recyclerView.setAdapter(adapter);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } , 1000);
        }

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
                recyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        var launchIntent = requireActivity().getPackageManager()
                                .getLaunchIntentForPackage("org.qp.android.plugin");
                        if (launchIntent != null) {
                            startActivity(launchIntent);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                    }
                }));
    }

    private void fillPluginList(@NonNull PluginClient pluginClient) {
        pluginClient.loadListPlugin(requireContext());
        namePlugin = pluginClient.getNamePlugin();
        pluginClient.getServicesLiveData().observe(getViewLifecycleOwner() ,
                hashMaps -> services = hashMaps);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().registerReceiver(packageBroadcastReceiver, packageFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(packageBroadcastReceiver);
    }

    class PackageBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            services.clear();
            fillPluginList(new PluginClient());
        }
    }
}
