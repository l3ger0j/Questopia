package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.qsp.player.databinding.FragmentStockAllBinding;
import com.qsp.player.viewModel.viewModels.AllStockFragmentViewModel;

public class AllStockFragment extends Fragment {
    private AllStockFragmentViewModel allViewModel;

    public AllStockFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentStockAllBinding fragmentStockAllBinding =
                FragmentStockAllBinding.inflate(getLayoutInflater());

        allViewModel = new ViewModelProvider(requireActivity())
                .get(AllStockFragmentViewModel.class);

        return fragmentStockAllBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
    }
}
