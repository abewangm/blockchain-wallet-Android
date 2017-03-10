package piuk.blockchain.android;

import android.support.annotation.CallSuper;

import org.junit.After;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;

/**
 * Class that applies a {@link TestScheduler} to the Rx computation thread, allowing {@link
 * Observable#interval(long, long, TimeUnit)} objects to be more easily tested.
 */
public class RxIntervalTest {

    protected TestScheduler testScheduler;

    @Before
    @CallSuper
    public void setUp() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();

        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());

        RxJavaPlugins.setInitIoSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setInitComputationSchedulerHandler(schedulerCallable -> testScheduler);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setInitSingleSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }
}

