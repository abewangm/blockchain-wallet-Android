package piuk.blockchain.android.ui.customviews;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Allows programmatic locking to current page
 */
public class NonSwipeableViewPager extends ViewPager {

    private boolean locked;

    public void lockToCurrentPage() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public NonSwipeableViewPager(Context context) {
        super(context);
    }

    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !locked && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return !locked && super.onTouchEvent(event);
    }
}