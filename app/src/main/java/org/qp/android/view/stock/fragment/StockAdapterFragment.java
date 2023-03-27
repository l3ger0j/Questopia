package org.qp.android.view.stock.fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class StockAdapterFragment extends FragmentStateAdapter {

    public StockAdapterFragment(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (StockAdFragment.newInstance(position));
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
