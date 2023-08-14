package org.qp.android.ui.news;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class NewsAdapterFragment extends FragmentStateAdapter {
    public NewsAdapterFragment(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return (NewsBannerFragment.newInstance(position));
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
