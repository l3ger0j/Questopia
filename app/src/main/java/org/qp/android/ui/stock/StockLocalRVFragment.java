package org.qp.android.ui.stock;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

public class StockLocalRVFragment extends Fragment {

    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    private int unselectColor;
    private int selectColor;

    private FragmentRecyclerBinding recyclerBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerBinding = FragmentRecyclerBinding.inflate(inflater, container, false);

        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);
        mRecyclerView = recyclerBinding.shareRecyclerView;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        unselectColor = android.R.attr.selectableItemBackground;
        selectColor = ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer);

        stockViewModel.gameEntriesLiveData.observe(getViewLifecycleOwner(), gameEntries -> {
            var adapter = new LocalGamesListAdapter().submitList(gameEntries);
            mRecyclerView.setAdapter(adapter);
        });

        stockViewModel.emitter.observe(getViewLifecycleOwner(), eventNavigation -> {
            if (eventNavigation instanceof StockFragmentNavigation.SelectAllElements) {
                selectAllElements();
            }
            if (eventNavigation instanceof StockFragmentNavigation.UnselectAllElements) {
                unselectAllElements();
            }
        });

        return recyclerBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerBinding = null;
    }

    private void selectAllElements() {
        for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final var holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            var constraintLayout = (ConstraintLayout) holder.itemView.findViewById(R.id.relativeLayout);
            constraintLayout.setBackgroundColor(selectColor);
        }
    }

    private void unselectAllElements() {
        for (int childCount = mRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final var holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            var constraintLayout = (ConstraintLayout) holder.itemView.findViewById(R.id.relativeLayout);
            constraintLayout.setBackgroundColor(unselectColor);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(requireContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (!stockViewModel.isEnableDeleteMode) {
                    stockViewModel.doOnShowGameFragment(position);
                } else {
                    var mViewHolder = mRecyclerView.findViewHolderForAdapterPosition(position);
                    if (mViewHolder == null) return;

                    var adapterPosition = mViewHolder.getAbsoluteAdapterPosition();
                    var gameEntriesList = stockViewModel.currInstalledGamesList;
                    if (adapterPosition == NO_POSITION) return;
                    if (adapterPosition < 0 || adapterPosition >= gameEntriesList.size()) return;

                    var gamesSelList = stockViewModel.selGameEntriesList;
                    var gameEntry = gameEntriesList.get(adapterPosition);
                    if (gamesSelList.isEmpty() || !gamesSelList.contains(gameEntry)) {
                        gamesSelList.add(gameEntry);
                        var constraintLayout = (ConstraintLayout) mViewHolder.itemView.findViewById(R.id.relativeLayout);
                        constraintLayout.setBackgroundColor(selectColor);
                    } else {
                        gamesSelList.remove(gameEntry);
                        var constraintLayout = (ConstraintLayout) mViewHolder.itemView.findViewById(R.id.relativeLayout);
                        constraintLayout.setBackgroundColor(unselectColor);
                    }
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
                stockViewModel.doOnShowActionMode();
            }
        }));
    }
}
