package org.qp.android.view.stock.fragment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public abstract class StockPatternFragment extends Fragment {

    public interface StockPatternFragmentList {
        void onClickEditButton ();
        void onClickPlayButton ();
        void onItemClick (int position);
        void onLongItemClick ();
        void onScrolled (RecyclerView recyclerView , int dx , int dy);
        void onScrollStateChanged (RecyclerView recyclerView , int newState);
    }

    public StockPatternFragment.StockPatternFragmentList listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (StockPatternFragment.StockPatternFragmentList) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PatternDialogListener");
        }
    }
}
