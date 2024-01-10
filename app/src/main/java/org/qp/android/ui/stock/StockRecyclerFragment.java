package org.qp.android.ui.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

import java.util.ArrayList;
import java.util.Objects;

public class StockRecyclerFragment extends StockPatternFragment {

    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    Observer<ArrayList<GameData>> gameData = gameData -> {
        var adapter = new StockGamesRecycler(requireActivity());
        adapter.submitList(gameData);
        mRecyclerView.setAdapter(adapter);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        org.qp.android.databinding.FragmentRecyclerBinding recyclerBinding =
                FragmentRecyclerBinding.inflate(inflater);
        mRecyclerView = recyclerBinding.shareRecyclerView;
        stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        stockViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        Objects.requireNonNull(stockViewModel.activityObservableField.get())
                .setRecyclerView(mRecyclerView);
        return recyclerBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        listener.onItemClick(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        listener.onLongItemClick();
                    }
                }));
    }
}
