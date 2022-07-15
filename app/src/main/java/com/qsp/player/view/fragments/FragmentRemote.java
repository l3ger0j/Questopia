package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.databinding.FragmentRemoteBinding;
import com.qsp.player.viewModel.viewModels.FragmentRemoteVM;

public class FragmentRemote extends Fragment {
    private FragmentRemoteVM remoteViewModel;

    public FragmentRemote() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentRemoteBinding fragmentStockRemoteBinding =
                FragmentRemoteBinding.inflate(getLayoutInflater());
        remoteViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentRemoteVM.class);

        return fragmentStockRemoteBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {

    }
}
