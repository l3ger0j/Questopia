package org.qp.android.ui.stock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StockStateAdapter extends FragmentStateAdapter {

    private final static int COUNT_FRAGMENTS = 2;

    @Override
    public int getItemCount() {
        return COUNT_FRAGMENTS;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new StockRecyclerFragment();
    }

    public StockStateAdapter(@NonNull FragmentManager fragmentManager,
                             @NonNull Lifecycle lifecycle) {
        super(fragmentManager , lifecycle);
    }

}
