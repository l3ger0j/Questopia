package org.qp.android.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

public class GameObjectFragment extends Fragment {

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
        viewModel.getObjectsObserver().observe(getViewLifecycleOwner() , gameItemRecycler -> {
            recyclerView.setBackgroundColor(viewModel.getBackgroundColor());
            recyclerBinding.shareRecyclerView.setAdapter(gameItemRecycler);
        });

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
