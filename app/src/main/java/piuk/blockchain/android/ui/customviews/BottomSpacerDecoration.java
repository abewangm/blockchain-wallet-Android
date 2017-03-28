package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import piuk.blockchain.android.R;

/**
 * For adding a spacer of a passed height (in pixels) to the bottom of a {@link RecyclerView}. Also
 * draws a gray underline beneath the last item.
 */
public class BottomSpacerDecoration extends RecyclerView.ItemDecoration {

    private final int height;
    private final Drawable divider;

    public BottomSpacerDecoration(Context context, int heightInPixels) {
        height = heightInPixels;
        divider = ContextCompat.getDrawable(context, R.drawable.divider_gray_light);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getAdapter().getItemCount();
        // If not last position in list, don't offset
        if (parent.getChildLayoutPosition(view) != childCount - 1) {
            return;
        }

        // Set bottom offset for last item in list
        outRect.set(0, 0, 0, height);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            // If not last position in list, don't draw divider
            if (i != childCount - 1) continue;

            View child = parent.getChildAt(i);

            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
    }
}
