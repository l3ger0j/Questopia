package org.qp.android.ui.stock;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;
import org.qp.android.helpers.bus.Events;

public class StockLocalRVFragment extends Fragment {

    private final LocalGamesListAdapter adapter = new LocalGamesListAdapter();
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

        return recyclerBinding.getRoot();
    }

    private void changeElementColorToDKGray() {
        for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final var holder =
                    mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
            cardView.setCardBackgroundColor(Color.DKGRAY);
        }
    }

    private void changeElementColorToLTGray() {
        for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final var holder =
                    mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
            cardView.setCardBackgroundColor(Color.LTGRAY);
        }
    }

    private void unselectOnce(int position) {
        final var holder =
                mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(position));
        var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
        cardView.setCardBackgroundColor(Color.DKGRAY);
    }

    private void selectOnce(int position) {
        final var holder =
                mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(position));
        var cardView = (CardView) holder.itemView.findViewWithTag("gameCardView");
        cardView.setCardBackgroundColor(Color.LTGRAY);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        stockViewModel.localDataList.observe(getViewLifecycleOwner(), adapter::submitList);
        stockViewModel.fragLocalRVEmit.observe(getViewLifecycleOwner(), new Events.EventObserver(event -> {
            if (event instanceof StockFragmentNavigation.ChangeElementColorToDKGray) {
                changeElementColorToDKGray();
            }
            if (event instanceof StockFragmentNavigation.ChangeElementColorToLTGray) {
                changeElementColorToLTGray();
            }
            if (event instanceof StockFragmentNavigation.SelectOnce once) {
                selectOnce(once.position);
            }
            if (event instanceof StockFragmentNavigation.UnselectOnce unselect) {
                unselectOnce(unselect.position);
            }
        }));

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext(),
                mRecyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (stockViewModel.isEnableDeleteMode) {
                            stockViewModel.onListItemClick(position);
                        } else {
                            var entryToShow = adapter.getItem(position);
                            stockViewModel.onListItemClick(entryToShow);
                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        stockViewModel.onLongListItemClick();
                    }
                }));
    }
}
