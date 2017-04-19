package piuk.blockchain.android;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;
import android.view.MotionEvent;

import org.junit.After;
import org.junit.Before;

import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.util.PrefsUtil.KEY_OVERLAY_TRUSTED;

@SuppressWarnings("WeakerAccess")
public class BaseEspressoTest {

    private SystemAnimations systemAnimations;
    private PrefsUtil prefsUtil;

    @CallSuper
    @Before
    public void setup() {
        systemAnimations = new SystemAnimations(InstrumentationRegistry.getTargetContext());
        prefsUtil = new PrefsUtil(InstrumentationRegistry.getTargetContext());
        clearState();
        ignoreTapJacking(true);
        disableAnimations();
    }

    @CallSuper
    @After
    public void tearDown() {
        enableAnimations();
    }

    /**
     * Clears application state completely for use between tests. Use alongside <code>new
     * ActivityTestRule<>(Activity.class, false, false)</code> and launch activity manually on setup
     * to avoid Espresso starting your activity automatically, if that's what you need.
     */
    protected void clearState() {
        prefsUtil.clear();
    }

    /**
     * Sets SharedPreferences value which means that {@link AppUtil#detectObscuredWindow(Context,
     * MotionEvent)} won't trigger a warning dialog.
     *
     * @param ignore Set to true to ignore all touch events
     */
    protected void ignoreTapJacking(boolean ignore) {
        prefsUtil.setValue(KEY_OVERLAY_TRUSTED, ignore);
    }

    /**
     * Disables all system animations for less flaky tests.
     */
    private void disableAnimations() {
        systemAnimations.disableAll();
    }

    /**
     * Re-enables all system animations. Intended for use once all tests complete.
     */
    private void enableAnimations() {
        systemAnimations.enableAll();
    }

}
