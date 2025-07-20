package org.qp.android.presentation.game;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class GameActionRecyclerView extends RecyclerView {
    private int maxVisibleItems = Integer.MAX_VALUE;

    public GameActionRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameActionRecyclerView(@NonNull Context context) {
        super(context);
    }

    public GameActionRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMaxVisibleItems(int count) {
        this.maxVisibleItems = count;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        var totalHeight = 0;
        var adapter = getAdapter();
        var manager = getLayoutManager();

        if (adapter != null && manager != null) {
            var items = Math.min(adapter.getItemCount(), maxVisibleItems);
            for (var i = 0; i < items; i++) {
                var child = manager.findViewByPosition(i);
                if (child == null) {
                    var vh = adapter.createViewHolder(this, adapter.getItemViewType(i));
                    adapter.bindViewHolder(vh, i);
                    child = vh.itemView;
                    child.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                } else {
                    child.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                }
                totalHeight += child.getMeasuredHeight();
            }
        }
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY));
    }
}
