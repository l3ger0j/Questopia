package org.qp.android.ui.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.databinding.FragmentStockGameBinding;

public class StockGameFragment extends StockPatternFragment {
    private FragmentStockGameBinding fragmentStockGameBinding;
    private StockViewModel stockViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater , @Nullable ViewGroup container , @Nullable Bundle savedInstanceState) {
        var appCompatActivity = ((AppCompatActivity) requireActivity());
        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        fragmentStockGameBinding = FragmentStockGameBinding.inflate(getLayoutInflater());
        stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        fragmentStockGameBinding.setViewModel(stockViewModel);
        return fragmentStockGameBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view , savedInstanceState);
        fragmentStockGameBinding.editButton.setOnClickListener(view1 -> listener.onClickEditButton());
        fragmentStockGameBinding.playButton.setOnClickListener(view2 -> listener.onClickPlayButton());
        fragmentStockGameBinding.downloadButton.setOnClickListener(view3 -> listener.onClickDownloadButton());
    }
}
