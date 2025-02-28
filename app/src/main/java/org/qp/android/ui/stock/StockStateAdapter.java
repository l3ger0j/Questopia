package org.qp.android.ui.stock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StockStateAdapter extends FragmentStateAdapter {

    private final static int COUNT_FRAGMENTS = 2;

    public StockStateAdapter(@NonNull FragmentManager fragmentManager,
                             @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @Override
    public int getItemCount() {
        return COUNT_FRAGMENTS;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new StockLocalRVFragment();
        } else {
            return new StockRemoteRVFragment();
        }
    }

}
