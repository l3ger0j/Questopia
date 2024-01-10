package org.qp.android.helpers.adapters;

import androidx.viewpager2.widget.ViewPager2;

public record AutoScrollRunnable(ViewPager2 viewPager ,
                                 int delayMillis ,
                                 boolean reverse) implements Runnable {
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