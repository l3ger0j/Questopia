package org.qp.android.view.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.view.settings.SettingsActivity;
import org.qp.android.viewModel.FragmentPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public class PluginFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();

    private FragmentPlugin pluginViewModel;
    private RecyclerView recyclerView;

    public static final String ACTION_PICK_PLUGIN = "org.qp.intent.action.PICK_PLUGIN";
    private static final String KEY_PKG = "pkg";
    private static final String KEY_SERVICENAME = "servicename";
    private static final String KEY_ACTIONS = "actions";
    private static final String KEY_CATEGORIES = "categories";
    private ArrayList<HashMap<String, String>> services;
    private ArrayList<String> categories;

    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;
    private String namePlugin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.pluginMenuTitle);
        packageBroadcastReceiver = new PackageBroadcastReceiver();
        packageFilter = new IntentFilter();
        var fragmentPluginBinding =
                FragmentPluginBinding.inflate(getLayoutInflater());
        recyclerView = fragmentPluginBinding.pluginRecyclerView;
        pluginViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentPlugin.class);
        fragmentPluginBinding.setPluginViewModel(pluginViewModel);
        pluginViewModel.fragmentObservableField.set(this);
        refreshPluginInfo();
        return fragmentPluginBinding.getRoot();
    }

    public void refreshPluginInfo() {
        fillPluginList();
        var pluginInfo = new PluginInfo();
        pluginInfo.title = namePlugin;
        var arrayList = new ArrayList<PluginInfo>();
        arrayList.add(pluginInfo);

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");

        var adapter =
                new PluginRecycler(requireActivity());
        adapter.submitList(arrayList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
                recyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        if (namePlugin != null) {
                            new PluginClient().connectPlugin(requireContext() , namePlugin);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                    }
                }));
    }

    private void fillPluginList() {
        services = new ArrayList<>();
        categories = new ArrayList<>();
        var packageManager = requireActivity().getPackageManager();
        var baseIntent = new Intent(ACTION_PICK_PLUGIN);
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        var list = packageManager.queryIntentServices(baseIntent,
                PackageManager.GET_RESOLVED_FILTER);
        Log.d(TAG, "fillPluginList: " + list);
        for (int i = 0; i < list.size(); ++i) {
            var info = list.get(i);
            var serviceInfo = info.serviceInfo;
            var filter = info.filter;
            Log.d(TAG, "fillPluginList: i: " + i + "; serviceInfo: " + serviceInfo + ";filter: " + filter);
            if (serviceInfo != null) {
                var item = new HashMap<String, String>();
                item.put(KEY_PKG, serviceInfo.packageName);
                namePlugin = serviceInfo.packageName;
                item.put(KEY_SERVICENAME, serviceInfo.name);
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
                categories.add(firstCategory);
                services.add(item);
            }
        }
        Log.d(TAG, "services: " + services);
        Log.d(TAG, "categories: " + categories);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().setTitle(R.string.settingsTitle);
        ((SettingsActivity) requireActivity()).onClickShowPlugin(false);
    }

    class PackageBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            services.clear();
            fillPluginList();
        }
    }
}
