package org.qp.android.view.stock.fragment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class StockPatternFragment extends Fragment {

    public interface StockPatternFragmentList {
        void onClickEditButton ();
        void onClickPlayButton ();
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
