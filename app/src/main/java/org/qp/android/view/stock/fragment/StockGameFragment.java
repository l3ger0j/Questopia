package org.qp.android.view.stock.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.databinding.FragmentStockGameBinding;
import org.qp.android.viewModel.FragmentStockGame;

public class StockGameFragment extends StockPatternFragment {
    private FragmentStockGameBinding fragmentStockGameBinding;
    private FragmentStockGame fragmentStockGame;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater , @Nullable ViewGroup container , @Nullable Bundle savedInstanceState) {
        var appCompatActivity = ((AppCompatActivity) requireActivity());
        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        fragmentStockGameBinding = FragmentStockGameBinding.inflate(getLayoutInflater());
        fragmentStockGame = new ViewModelProvider(requireActivity())
                .get(FragmentStockGame.class);
        fragmentStockGame.fragmentObservableField.set(this);
        fragmentStockGameBinding.setViewModel(fragmentStockGame);
        return fragmentStockGameBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view , savedInstanceState);
        fragmentStockGameBinding.editButton.setOnClickListener(view1 -> listener.onClickEditButton());
        fragmentStockGameBinding.playButton.setOnClickListener(view2 -> listener.onClickPlayButton());
    }
}
