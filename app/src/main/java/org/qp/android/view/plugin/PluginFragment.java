package org.qp.android.view.plugin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.viewModel.viewModels.FragmentPlugin;

import java.util.ArrayList;

public class PluginFragment extends Fragment {
    private FragmentPlugin pluginViewModel;
    private RecyclerView recyclerView;

    public PluginFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.pluginMenuTitle);
        org.qp.android.databinding.FragmentPluginBinding fragmentPluginBinding =
                FragmentPluginBinding.inflate(getLayoutInflater());
        recyclerView = fragmentPluginBinding.pluginRecyclerView;
        pluginViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentPlugin.class);
        pluginViewModel.getGameData().observe(getViewLifecycleOwner(), pluginList);
        return fragmentPluginBinding.getRoot();
    }

    Observer<ArrayList<PluginInfo>> pluginList = new Observer<ArrayList<PluginInfo>>() {
        @Override
        public void onChanged(ArrayList<PluginInfo> pluginInfos) {
            PluginRecycler adapter =
                    new PluginRecycler(requireActivity());
            adapter.submitList(pluginInfos);
            recyclerView.setAdapter(adapter);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
                recyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                    }
                }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().setTitle(R.string.settingsTitle);
    }
}
