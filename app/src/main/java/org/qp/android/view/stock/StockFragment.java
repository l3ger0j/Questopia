package org.qp.android.view.stock;

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

import org.qp.android.databinding.FragmentStockBinding;
import org.qp.android.dto.stock.GameData;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.viewModel.viewModels.FragmentStock;

import java.util.ArrayList;
import java.util.Objects;

public class StockFragment extends Fragment {
    private FragmentStock localViewModel;
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        org.qp.android.databinding.FragmentStockBinding fragmentStockBinding =
                FragmentStockBinding.inflate(getLayoutInflater());
        mRecyclerView = fragmentStockBinding.gamesLocal;
        localViewModel = new ViewModelProvider(requireActivity())
                .get(FragmentStock.class);
        localViewModel.getGameData().observe(getViewLifecycleOwner(), gameData);
        Objects.requireNonNull(localViewModel.activityObservableField.get())
                .setRecyclerView(mRecyclerView);
        return fragmentStockBinding.getRoot();
    }

    Observer<ArrayList<GameData>> gameData = new Observer<ArrayList<GameData>>() {
        @Override
        public void onChanged(ArrayList<GameData> gameData) {
            GamesRecycler adapter =
                    new GamesRecycler(requireActivity());
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
                                .get()).onLongItemClick();
                    }
                }));
    }
}
