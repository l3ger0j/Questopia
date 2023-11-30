package org.qp.android.ui.stock;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class StockPatternFragment extends Fragment {

    public interface StockPatternFragmentList {
        void onClickEditButton();
        void onClickPlayButton();
        void onClickDownloadButton();
        void onItemClick(int position);
        void onLongItemClick();
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
