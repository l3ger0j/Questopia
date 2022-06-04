package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qsp.player.R;
import com.qsp.player.databinding.FragmentStockAllBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;
import com.qsp.player.view.adapters.RecyclerItemClickListener;
import com.qsp.player.viewModel.viewModels.AllStockFragmentViewModel;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Objects;

public class AllStockFragment extends Fragment {
    private AllStockFragmentViewModel allViewModel;
    private GamesRecyclerAdapter adapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    public AllStockFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentStockAllBinding fragmentStockAllBinding =
                FragmentStockAllBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockAllBinding.gamesAll;
        mProgressBar = fragmentStockAllBinding.progressBar;
        allViewModel = new ViewModelProvider(requireActivity())
                .get(AllStockFragmentViewModel.class);

        mProgressBar.setIndeterminate(true);

        allViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        return fragmentStockAllBinding.getRoot();
    }

    Observer<ArrayList<GameData>> gameData = new Observer<ArrayList<GameData>>() {
        @Override
        public void onChanged(ArrayList<GameData> gameData) {
            adapter = new GamesRecyclerAdapter(requireActivity());
            adapter.submitList(gameData);
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressBar.setIndeterminate(false);
            mProgressBar.setVisibility(View.GONE);
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
                        Objects.requireNonNull(allViewModel.activityObservableField
                                .get()).onItemClick(position, getTag());
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        Objects.requireNonNull(allViewModel.activityObservableField
                                .get()).onLongItemClick(position, getTag());
                    }
                }));
    }
}
