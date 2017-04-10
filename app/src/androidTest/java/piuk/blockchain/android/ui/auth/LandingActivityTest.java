package piuk.blockchain.android.ui.auth;

import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import piuk.blockchain.android.BaseEspressoTest;
import piuk.blockchain.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class LandingActivityTest extends BaseEspressoTest {

    private static final ViewInteraction BUTTON_LOGIN = onView(withId(R.id.login));
    private static final ViewInteraction BUTTON_CREATE = onView(withId(R.id.create));
    private static final ViewInteraction BUTTON_RECOVER = onView(withId(R.id.recover_funds));

    @Rule
    public ActivityTestRule<LandingActivity> activityRule =
            new ActivityTestRule<>(LandingActivity.class);

    @Test
    public void isLaunched() throws Exception {
        assertTrue(activityRule.getActivity() != null);
    }

    @Test
    public void launchLoginPage() throws InterruptedException {
        BUTTON_LOGIN.perform(click());
        // Check pairing fragment launched
        onView(withText(R.string.pair_your_wallet)).check(matches(isDisplayed()));
    }

    @Test
    public void launchCreateWalletPage() throws InterruptedException {
        BUTTON_CREATE.perform(click());
        // Check create wallet fragment launched
        onView(withText(R.string.new_wallet)).check(matches(isDisplayed()));
    }

    @Test
    public void launchRecoverFundsPage() throws InterruptedException {
        BUTTON_RECOVER.perform(click());
        // Verify warning dialog showing
        onView(withText(R.string.recover_funds_warning_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        // Click "Continue"
        onView(withId(android.R.id.button1)).perform(click());
        // Check recover funds activity launched
        onView(withText(R.string.recover_funds)).check(matches(isDisplayed()));
    }

}