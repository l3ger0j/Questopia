package com.qsp.player.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.qsp.player.databinding.FragmentStockRemoteBinding;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;
import com.qsp.player.view.adapters.RecyclerItemClickListener;
import com.qsp.player.viewModel.viewModels.RemoteStockFragmentViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class RemoteStockFragment extends Fragment {
    private RemoteStockFragmentViewModel remoteViewModel;
    private GamesRecyclerAdapter adapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    public RemoteStockFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        com.qsp.player.databinding.FragmentStockRemoteBinding fragmentStockRemoteBinding =
                FragmentStockRemoteBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockRemoteBinding.gamesRemote;
        mProgressBar = fragmentStockRemoteBinding.progressBar;
        remoteViewModel = new ViewModelProvider(requireActivity())
                .get(RemoteStockFragmentViewModel.class);

        mProgressBar.setIndeterminate(true);

        remoteViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        return fragmentStockRemoteBinding.getRoot();
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
                        Objects.requireNonNull(remoteViewModel.activityObservableField
                                .get()).onItemClick(position, getTag());
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {
                        Objects.requireNonNull(remoteViewModel.activityObservableField
                                .get()).onLongItemClick(position, getTag());
                    }
                }));
    }
}
