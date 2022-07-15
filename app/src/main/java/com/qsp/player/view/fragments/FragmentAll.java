package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.databinding.FragmentAllBinding;
import com.qsp.player.viewModel.viewModels.FragmentAllVM;

public class FragmentAll extends Fragment {
    private FragmentAllVM allViewModel;
    private FragmentAllBinding fragmentAllBinding;

    public FragmentAll() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        fragmentAllBinding = FragmentAllBinding.inflate(getLayoutInflater());

        allViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentAllVM.class);

        return fragmentAllBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
    }
}
