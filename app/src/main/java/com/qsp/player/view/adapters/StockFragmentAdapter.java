package com.qsp.player.view.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.qsp.player.view.fragments.AllStockFragment;
import com.qsp.player.view.fragments.LocalStockFragment;
import com.qsp.player.view.fragments.RemoteStockFragment;

public class StockFragmentAdapter extends FragmentStateAdapter {
    public StockFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new RemoteStockFragment();
            case 2:
                return new AllStockFragment();
            default:
                return new LocalStockFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
