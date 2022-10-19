package com.qsp.player.view.fragments;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qsp.player.databinding.FragmentPluginBinding;
import com.qsp.player.dto.PluginList;
import com.qsp.player.view.adapters.PluginRecycler;
import com.qsp.player.view.adapters.RecyclerItemClickListener;
import com.qsp.player.viewModel.viewModels.FragmentPlugin;

import java.util.ArrayList;

public class Plugin extends Fragment {
    private FragmentPlugin pluginViewModel;
    private FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;

    public Plugin () {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle("Plugins");
        com.qsp.player.databinding.FragmentPluginBinding fragmentPluginBinding =
                FragmentPluginBinding.inflate(getLayoutInflater());
        floatingActionButton = fragmentPluginBinding.pluginFAB;
        recyclerView = fragmentPluginBinding.pluginRecyclerView;
        pluginViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentPlugin.class);
        pluginViewModel.getGameData().observe(getViewLifecycleOwner(), pluginList);

        return fragmentPluginBinding.getRoot();
    }

    Observer<ArrayList<PluginList>> pluginList = new Observer<ArrayList<PluginList>>() {
        @Override
        public void onChanged(ArrayList<PluginList> pluginLists) {
            PluginRecycler adapter =
                    new PluginRecycler(requireActivity());
            adapter.submitList(pluginLists);
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
}
