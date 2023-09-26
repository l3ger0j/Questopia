package org.qp.android.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

public class GameObjectFragment extends Fragment {

    private FragmentRecyclerBinding recyclerBinding;
    private GameViewModel viewModel;
    private RecyclerView objectView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // RecyclerView
        objectView = recyclerBinding.shareRecyclerView;
        var manager = (LinearLayoutManager) objectView.getLayoutManager();
        var dividerItemDecoration = new DividerItemDecoration(
                objectView.getContext() ,
                manager.getOrientation());
        objectView.addItemDecoration(dividerItemDecoration);
        objectView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewModel.getObjectsObserver().observe(getViewLifecycleOwner() , gameItemRecycler -> {
            objectView.setBackgroundColor(viewModel.getBackgroundColor());
            recyclerBinding.shareRecyclerView.setAdapter(gameItemRecycler);
        });

        // Settings
        viewModel.getControllerObserver().observe(getViewLifecycleOwner() , settingsController -> {
            objectView.setBackgroundColor(viewModel.getBackgroundColor());
            recyclerBinding.getRoot().refreshDrawableState();
        });
        return recyclerBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        objectView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                objectView ,
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
