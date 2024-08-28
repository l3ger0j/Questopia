package org.qp.android.ui.stock;

import static org.qp.android.ui.stock.GamesListAdapter.DIVIDER;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DividerDecoration extends RecyclerView.ItemDecoration {

    private final Paint mPaint;
    private final int mHeightDp;

    public DividerDecoration(Context context) {
        this(context, Color.argb((int) (255 * 0.2), 0, 0, 0), 1f);
    }

    public DividerDecoration(Context context, int color, float heightDp) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
        mHeightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, context.getResources().getDisplayMetrics());
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        var position = parent.getChildAdapterPosition(view);
        if (parent.getAdapter() == null) return;
        var viewType = parent.getAdapter().getItemViewType(position);

        if (viewType == DIVIDER) {
            outRect.set(0, 0, 0, mHeightDp);
        } else {
            outRect.setEmpty();
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c,
                       @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            var view = parent.getChildAt(i);
            var position = parent.getChildAdapterPosition(view);
            if (parent.getAdapter() == null) continue;
            var viewType = parent.getAdapter().getItemViewType(position);

            if (viewType == DIVIDER) {
                var nextView = parent.getChildAt(i + 1);
                var rect = new RectF(view.getLeft(), view.getBottom() + mHeightDp, view.getRight(), nextView.getTop() - mHeightDp);
                c.drawRoundRect(rect, 10f, 10f, mPaint);
            }
        }
    }
}
