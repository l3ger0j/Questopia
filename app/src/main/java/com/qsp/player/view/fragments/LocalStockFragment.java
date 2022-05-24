package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.qsp.player.databinding.FragmentStockLocalBinding;
import com.qsp.player.view.adapters.RecyclerItemClickListener;
import com.qsp.player.viewModel.viewModels.StockViewModel;

import java.util.Objects;

public class LocalStockFragment extends Fragment {
    private RecyclerView mRecyclerView;

    public LocalStockFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentStockLocalBinding fragmentStockLocalBinding =
                FragmentStockLocalBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockLocalBinding.gamesLocal;
        return fragmentStockLocalBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        StockViewModel stockViewModel = new ViewModelProvider(requireActivity())
                .get(StockViewModel.class);
        stockViewModel.getRecyclerAdapter().observe(getViewLifecycleOwner() ,
                gamesRecyclerAdapter -> mRecyclerView.setAdapter(gamesRecyclerAdapter));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        Objects.requireNonNull(stockViewModel.activityObservableField
                                .get()).onItemClick(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        Objects.requireNonNull(stockViewModel.activityObservableField
                                .get()).onLongItemClick(position);
                    }
                }));
    }
}
