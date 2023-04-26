package org.qp.android.view.game.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.view.adapters.RecyclerItemClickListener;
import org.qp.android.viewModel.GameViewModel;

public class GameObjectFragment extends GamePatternFragment {
    private FragmentRecyclerBinding recyclerBinding;
    private GameViewModel viewModel;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // RecyclerView
        recyclerView = recyclerBinding.shareRecyclerView;
        recyclerView.setBackgroundColor(viewModel.getBackgroundColor());
        viewModel.getObjectsObserver().observe(getViewLifecycleOwner() , gameItemRecycler ->
                recyclerBinding.shareRecyclerView.setAdapter(gameItemRecycler));

        // Settings
        viewModel.getControllerObserver().observe(getViewLifecycleOwner() , settingsController -> {
            recyclerView.setBackgroundColor(viewModel.getBackgroundColor());
            recyclerBinding.getRoot().refreshDrawableState();
        });
        return recyclerBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                recyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view , int position) {
                        viewModel.getLibQspProxy().onObjectSelected(position);
                    }

                    @Override
                    public void onLongItemClick(View view , int position) {

                    }
                }
        ));
    }
}
