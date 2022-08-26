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

import com.qsp.player.databinding.FragmentLocalBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;
import com.qsp.player.view.adapters.RecyclerItemClickListener;
import com.qsp.player.viewModel.viewModels.FragmentLocalVM;

import java.util.ArrayList;
import java.util.Objects;

public class FragmentLocal extends Fragment {
    private FragmentLocalVM localViewModel;
    private RecyclerView mRecyclerView;

    public FragmentLocal() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentLocalBinding fragmentStockLocalBinding =
                FragmentLocalBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockLocalBinding.gamesLocal;
        localViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentLocalVM.class);
        localViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        Objects.requireNonNull(localViewModel.activityObservableField.get())
                .setRecyclerView(mRecyclerView);
        return fragmentStockLocalBinding.getRoot();
    }

    Observer<ArrayList<GameData>> gameData = new Observer<ArrayList<GameData>>() {
        @Override
        public void onChanged(ArrayList<GameData> gameData) {
            GamesRecyclerAdapter adapter =
                    new GamesRecyclerAdapter(requireActivity());
            adapter.submitList(gameData);
            mRecyclerView.setAdapter(adapter);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                getContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        Objects.requireNonNull(localViewModel.activityObservableField
                                .get()).onItemClick(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        Objects.requireNonNull(localViewModel.activityObservableField
                                .get()).onLongItemClick(position);
                    }
                }));
    }
}
