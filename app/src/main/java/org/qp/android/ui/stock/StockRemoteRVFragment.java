package org.qp.android.ui.stock;

import static org.qp.android.ui.stock.StockViewModel.FOLDER_CREATE;

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

import org.qp.android.databinding.FragmentRecyclerRemoteBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.ui.dialogs.StockDialogType;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class StockRemoteRVFragment extends Fragment {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;
    private FragmentRecyclerRemoteBinding recyclerBinding;
    private RemoteGamesAdapter remoteAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerRemoteBinding.inflate(inflater, container, false);

        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        mRecyclerView = recyclerBinding.shareRV.getRoot();

        var banner = recyclerBinding.remoteErrorBanner;
        banner.setLeftButton("Dismiss", banner1 -> {
            banner.dismiss(500);
        });

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

        stockViewModel.emitter.observe(getViewLifecycleOwner(), eventNavigation -> {
            if (eventNavigation instanceof StockFragmentNavigation.ShowErrorBanner errorBanner) {
                banner.setMessage(errorBanner.inputMessage);

                banner.setRightButton(errorBanner.rightButtonMsg, banner3 -> {
                    if (errorBanner.rightButtonMsg.equals(FOLDER_CREATE)) {
                        if (stockViewModel.doMakeGameDir(null)) {
                            banner.dismiss(500);
                        }
                    } else {
                        stockViewModel.showDialogFragment(
                                getChildFragmentManager(),
                                StockDialogType.GAME_FOLDER_INIT,
                                null,
                                null
                        );
                    }
                });
                banner.show(500);
            }
        });

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
