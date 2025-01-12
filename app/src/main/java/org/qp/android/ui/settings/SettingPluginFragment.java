package org.qp.android.ui.settings;

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
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.QuestopiaApplication;
import org.qp.android.R;
import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginType;
import org.qp.android.plugin.IQuestPlugin;
import org.qp.android.questopiabundle.IQuestopiaBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SettingPluginFragment extends Fragment {

    private RecyclerView recyclerView;
    private List<HashMap<String, String>> services;

    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;

    private IQuestPlugin questPlugin;
    private IQuestopiaBundle questEngine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        var shareRecyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        recyclerView = shareRecyclerBinding.shareRecyclerView;

        refreshPluginInfo();

        return shareRecyclerBinding.getRoot();
    }

    public void refreshPluginInfo() {
        var pluginClient = new PluginClient();
        var arrayList = new ArrayList<PluginInfo>();

        fillPluginList(pluginClient);

        new Handler().postDelayed(() -> services.forEach(stringStringHashMap -> stringStringHashMap.forEach((s, s2) -> {
            if (Objects.equals(s, "servicename")) {
                switch (s2) {
                    case "org.qp.android.plugin.AidlService" -> {
                        pluginClient.connectPlugin(requireContext(), PluginType.DOWNLOAD_PLUGIN);
                        new Handler().postDelayed(() -> {
                            questPlugin = pluginClient.questPlugin;
                            try {
                                var pluginInfo = new PluginInfo(
                                        questPlugin.versionPlugin(),
                                        questPlugin.titlePlugin(),
                                        questPlugin.authorPlugin()
                                );
                                arrayList.add(pluginInfo);
                            } catch (RemoteException e) {
                                Log.e(getTag(), "Error:", e);
                            }
                        }, 1000);
                    }
                    case "org.qp.android.questopiabundle.QuestopiaBundle" -> {
                        var client = ((QuestopiaApplication) requireActivity().getApplication()).getCurrPluginClient();
                        if (client != null) {
                            questEngine = client.questopiaBundle;
                            try {
                                var pluginInfo = new PluginInfo(
                                        questEngine.versionPlugin(),
                                        questEngine.titlePlugin(),
                                        questEngine.authorPlugin()
                                );
                                arrayList.add(pluginInfo);
                            } catch (Exception e) {
                                Log.e(getTag(), "Error:", e);
                            }
                        } else {
                            pluginClient.connectPlugin(requireContext(), PluginType.ENGINE_PLUGIN);
                            new Handler().postDelayed(() -> {
                                questEngine = pluginClient.questopiaBundle;
                                try {
                                    var pluginInfo = new PluginInfo(
                                            questEngine.versionPlugin(),
                                            questEngine.titlePlugin(),
                                            questEngine.authorPlugin()
                                    );
                                    arrayList.add(pluginInfo);
                                    questEngine.stopNativeLib();
                                } catch (Exception e) {
                                    Log.e(getTag(), "Error:", e);
                                }
                                pluginClient.disconnectPlugin(requireContext(), PluginType.ENGINE_PLUGIN);
                            }, 1000);
                        }
                    }
                }
            }
        })), 1000);

        new Handler().postDelayed(() -> {
            var adapter = new SettingPluginRecycler(requireActivity());
            adapter.submitList(arrayList);
            recyclerView.setAdapter(adapter);
        }, 3000);

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext(),
                recyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        var tv = (TextView) view.findViewById(R.id.plugin_title);
                        if (Objects.equals(String.valueOf(tv.getText()), "Downloader Plugin For QPA")) {
                            var launchIntent = requireActivity().getPackageManager()
                                    .getLaunchIntentForPackage("org.qp.android.plugin");
                            if (launchIntent != null) {
                                startActivity(launchIntent);
                            }
                        } else {
                            var pluginClient = new PluginClient();
                            pluginClient.connectPlugin(requireContext(), PluginType.ENGINE_PLUGIN);
                            ((QuestopiaApplication) requireActivity().getApplication()).setCurrPluginClient(pluginClient);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                }));
    }

    private void fillPluginList(@NonNull PluginClient pluginClient) {
        pluginClient.loadListPlugin(requireContext());
        pluginClient.getServicesLiveData().observe(getViewLifecycleOwner(), hashMaps -> services = hashMaps);
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
            services.clear();
            fillPluginList(new PluginClient());
        }
    }
}