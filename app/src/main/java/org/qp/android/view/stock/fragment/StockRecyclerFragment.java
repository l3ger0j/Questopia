package org.qp.android.view.stock.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentStockRecyclerBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.view.stock.StockGamesRecycler;
import org.qp.android.viewModel.StockViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class StockRecyclerFragment extends StockPatternFragment {
    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        org.qp.android.databinding.FragmentStockRecyclerBinding fragmentStockBinding =
                FragmentStockRecyclerBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockBinding.gamesLocal;
        stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        stockViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        Objects.requireNonNull(stockViewModel.activityObservableField.get())
                .setRecyclerView(mRecyclerView);
        return fragmentStockBinding.getRoot();
    }

    Observer<ArrayList<GameData>> gameData = new Observer<>() {
        @Override
        public void onChanged(ArrayList<GameData> gameData) {
            var adapter =
                    new StockGamesRecycler(requireActivity());
            adapter.submitList(gameData);
            mRecyclerView.setAdapter(adapter);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView , int dx , int dy) {
                listener.onScrolled(recyclerView, dx, dy);
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView , int newState) {
                listener.onScrollStateChanged(recyclerView, newState);
            }
        });
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
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
