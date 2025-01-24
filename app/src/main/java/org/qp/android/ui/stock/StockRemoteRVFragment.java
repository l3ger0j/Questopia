package org.qp.android.ui.stock;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class StockRemoteRVFragment extends Fragment {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;
    private FragmentRecyclerBinding recyclerBinding;
    private RemoteGamesAdapter remoteAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerBinding.inflate(inflater, container, false);

        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        mRecyclerView = recyclerBinding.shareRecyclerView;

        var orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        }

        remoteAdapter = new RemoteGamesAdapter();
        var disposable = stockViewModel.remoteDataFlow.subscribe(data -> {
            remoteAdapter.submitData(getLifecycle(), data);
        });
        compositeDisposable.add(disposable);

        mRecyclerView.setAdapter(
                remoteAdapter.withLoadStateFooter(
                        new RemoteGamesLoadStateAdapter(view -> remoteAdapter.retry()))
        );

        return recyclerBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerBinding = null;
        compositeDisposable.dispose();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext(),
                mRecyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        var entryToShow = remoteAdapter.getItemByPosition(position);
                        stockViewModel.doOnShowGameFragment(entryToShow);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // Do nothing
                    }
                }));
    }
}
