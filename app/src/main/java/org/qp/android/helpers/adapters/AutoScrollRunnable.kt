package org.qp.android.helpers.adapters;

import androidx.viewpager2.widget.ViewPager2;

public class AutoScrollRunnable implements Runnable {
    private final ViewPager2 viewPager;
    private final int delayMillis;
    private final boolean reverse;

    public AutoScrollRunnable(ViewPager2 viewPager,
                              int delayMillis,
                              boolean reverse) {
        this.viewPager = viewPager;
        this.delayMillis = delayMillis;
        this.reverse = reverse;
    }

    @Override
    public void run() {
        int currentItem = viewPager.getCurrentItem();
        if (viewPager.getAdapter() != null) {
            int itemCount = viewPager.getAdapter().getItemCount();
            int nextItem = reverse ? currentItem - 1 : currentItem + 1;
            if (nextItem >= itemCount) {
                nextItem = 0;
            } else if (nextItem < 0) {
                nextItem = itemCount - 1;
            }
            viewPager.setCurrentItem(nextItem , true);
            viewPager.postDelayed(this , delayMillis);
        }
    }
}
