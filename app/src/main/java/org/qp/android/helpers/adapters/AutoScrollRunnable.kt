package org.qp.android.helpers.adapters

import androidx.viewpager2.widget.ViewPager2

class AutoScrollRunnable(
    private val viewPager: ViewPager2,
    private val delayMillis: Int,
    private val reverse: Boolean
) : Runnable {

    override fun run() {
        val currentItem = viewPager.currentItem
        if (viewPager.adapter != null) {
            val itemCount = viewPager.adapter!!.itemCount
            var nextItem = if (reverse) currentItem - 1 else currentItem + 1
            if (nextItem >= itemCount) {
                nextItem = 0
            } else if (nextItem < 0) {
                nextItem = itemCount - 1
            }
            viewPager.setCurrentItem(nextItem, true)
            viewPager.postDelayed(this, delayMillis.toLong())
        }
    }
}