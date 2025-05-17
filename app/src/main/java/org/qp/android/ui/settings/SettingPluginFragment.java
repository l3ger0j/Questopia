package org.qp.android.ui.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.model.plugin.PluginService;
import org.qp.android.questopiabundle.IQuestopiaBundle;

import java.util.Collections;
import java.util.List;

public class SettingPluginFragment extends Fragment {

    private final PluginClient client = PluginService.client;
    private final MutableLiveData<List<PluginInfo>> infoPluginsLiveData = new MutableLiveData<>();
    private RecyclerView recyclerView;
    private SettingPluginAdapter pluginAdapter;
    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;
    private IQuestopiaBundle questopiaBundle = new IQuestopiaBundle.Default();
    private final ServiceConnection engineConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            questopiaBundle = IQuestopiaBundle.Stub.asInterface(service);

            try {
                infoPluginsLiveData.postValue(List.of(
                        new PluginInfo(
                                questopiaBundle.versionPlugin(),
                                questopiaBundle.titlePlugin(),
                                questopiaBundle.authorPlugin()
                        )
                ));
            } catch (RemoteException e) {
//                throw new RuntimeException(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            questopiaBundle = new IQuestopiaBundle.Default();
        }
    };

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

        client.connectEnginePlugin(requireContext(), engineConn);

        packageBroadcastReceiver = new PackageBroadcastReceiver();
        packageFilter = new IntentFilter();

        var shareRecyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        recyclerView = shareRecyclerBinding.shareRecyclerView;
        pluginAdapter = new SettingPluginAdapter();
        recyclerView.setAdapter(pluginAdapter);

        fillPluginList();

        return shareRecyclerBinding.getRoot();
    }

    private void refreshPluginInfo() {
        infoPluginsLiveData.observe(getViewLifecycleOwner(), pluginInfos -> {
            pluginAdapter.submitList(pluginInfos == null ? Collections.emptyList() : pluginInfos);
        });

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext(),
                recyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                }));
    }

    private void fillPluginList() {
        refreshPluginInfo();
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
    public void onDestroy() {
        super.onDestroy();
        client.disconnectEnginePlugin(requireContext());
    }

    class PackageBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            fillPluginList();
        }
    }
}