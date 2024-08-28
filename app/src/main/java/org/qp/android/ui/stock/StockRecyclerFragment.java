package org.qp.android.ui.stock;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.databinding.FragmentRecyclerBinding;
import org.qp.android.helpers.adapters.RecyclerItemClickListener;

public class StockRecyclerFragment extends Fragment {

    private StockViewModel stockViewModel;
    private RecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        var recyclerBinding = FragmentRecyclerBinding.inflate(inflater);
        mRecyclerView = recyclerBinding.shareRecyclerView;
        mRecyclerView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        stockViewModel.getGameDataList().observe(getViewLifecycleOwner(), item -> {
            var adapter = new GamesListAdapter(requireActivity()).submitList(item);
            var divider = new DividerDecoration(requireContext(), Color.GRAY, 5f);
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.addItemDecoration(divider);
        });

        stockViewModel.emitter.observe(getViewLifecycleOwner(), eventNavigation -> {
            if (eventNavigation instanceof StockFragmentNavigation.ChangeElementColorToDKGray) {
                changeElementColorToDKGray();
            }
            if (eventNavigation instanceof StockFragmentNavigation.ChangeElementColorToLTGray) {
                changeElementColorToLTGray();
            }
        });

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

    @Override
    public void onViewCreated(@NonNull View view , @Nullable Bundle savedInstanceState) {
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                requireContext() ,
                mRecyclerView ,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        var mViewHolder = mRecyclerView.findViewHolderForAdapterPosition(position);
                        if (mViewHolder == null) return;
                        stockViewModel.doOnSendAdapterViewHolder(mViewHolder);
                        stockViewModel.doOnShowGameFragment(position);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        stockViewModel.doOnShowActionMode();
                    }
                }));
        mRecyclerView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(@NonNull View host ,
                                                      int action ,
                                                      @Nullable Bundle args) {
                switch (action) {
                    case AccessibilityNodeInfo.ACTION_CLICK -> host.performClick();
                    case AccessibilityNodeInfo.ACTION_LONG_CLICK -> host.performLongClick();
                }
                return super.performAccessibilityAction(host , action , args);
            }
        });
    }
}
