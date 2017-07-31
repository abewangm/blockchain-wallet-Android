package piuk.blockchain.android.ui.customviews;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * For adding a spacer of a passed height (in pixels) to the bottom of a {@link RecyclerView}.
 */
public class BottomSpacerDecoration extends RecyclerView.ItemDecoration {

    private final int height;

    public BottomSpacerDecoration(int heightInPixels) {
        height = heightInPixels;
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

}
