package org.qp.android.presentation.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.helpers.bus.Events;

public class StockLocalRVFragment extends Fragment {

    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;
    private FragmentRecyclerBinding recyclerBinding;
    private LocalGamesListAdapter localAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerBinding.inflate(inflater, container, false);

        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        mRecyclerView = recyclerBinding.shareRecyclerView;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        localAdapter = new LocalGamesListAdapter();
        stockViewModel.gameEntriesLiveData.observe(getViewLifecycleOwner(), localAdapter::submitList);
        mRecyclerView.setAdapter(localAdapter);

        var selectionTracker = new SelectionTracker.Builder<>(
                "long-game-id",
                mRecyclerView,
                new LocalGamesListAdapter.LocalGamesIdsProvider(localAdapter),
                new LocalGamesListAdapter.LocalGamesDetailsLookup(mRecyclerView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build();
        selectionTracker.onRestoreInstanceState(savedInstanceState);
        localAdapter.setTracker(selectionTracker);

        selectionTracker.addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onItemStateChanged(@NonNull Long key, boolean selected) {
                stockViewModel.onUpdateSelection(key, selected);
            }

            @Override
            public void onSelectionChanged() {
                if (!selectionTracker.getSelection().isEmpty()) {
                    stockViewModel.onLongListItemClick();
                }
            }
        });

        stockViewModel.fragLocalRVEmit.observe(getViewLifecycleOwner(), new Events.EventObserver(event -> {
            if (event instanceof StockFragmentNavigation.ClearSelectElements) {
                selectionTracker.clearSelection();
            }
            if (event instanceof StockFragmentNavigation.ChangeStateElements) {
                stockViewModel.getListGamesFuture().thenAccept(games -> {
                    var idsList = games.stream().map(game -> game.id).toList();
                    if (stockViewModel.selGameEntriesSet.size() == games.size()) {
                        selectionTracker.clearSelection();
                        stockViewModel.selGameEntriesSet.clear();
                    } else {
                        selectionTracker.setItemsSelected(idsList, true);
                    }
                });
            }
        }));

        return recyclerBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        localAdapter = null;
        recyclerBinding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(requireContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (!stockViewModel.isEnableDeleteMode) {
                    var entry = localAdapter.getGameEntry(position);
                    stockViewModel.doOnShowGameFragment(entry);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
    }
}
