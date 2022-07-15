package com.qsp.player.view.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.qsp.player.view.fragments.FragmentAll;
import com.qsp.player.view.fragments.FragmentLocal;
import com.qsp.player.view.fragments.FragmentRemote;

public class StockFragmentAdapter extends FragmentStateAdapter {
    public StockFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new FragmentRemote();
            case 2:
                return new FragmentAll();
            default:
                return new FragmentLocal();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
