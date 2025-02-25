package org.qp.android.ui.stock;

import android.graphics.Color;
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

public class StockRemoteRVFragment extends Fragment {

    private final RemoteGamesListAdapter adapter = new RemoteGamesListAdapter();
    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var recyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        var divider = new DividerDecoration(requireContext(), Color.GRAY, 5f);

        mRecyclerView = recyclerBinding.shareRecyclerView;
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addItemDecoration(divider);

        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        stockViewModel.remoteDataList.observe(getViewLifecycleOwner(), adapter::submitList);

        return recyclerBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext(),
                mRecyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        var entryToShow = adapter.getItem(position);
                        stockViewModel.onListItemClick(entryToShow);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // do nothing
                    }
                }));
    }
}
