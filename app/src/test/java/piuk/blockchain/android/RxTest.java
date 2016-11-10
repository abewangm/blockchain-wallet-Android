package piuk.blockchain.android;

import android.support.annotation.CallSuper;

import org.junit.After;
import org.junit.Before;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by adambennett on 08/08/2016.
 *
 * Class that forces all Rx observables to be subscribed and observed in the same thread
 * through the same Scheduler that runs immediately.
 */
public class RxTest {

    @Before
    @CallSuper
    public void setUp() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();

        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> new TestScheduler());

        RxJavaPlugins.setInitIoSchedulerHandler(schedulerCallable -> new TestScheduler());
        RxJavaPlugins.onIoScheduler(new TestScheduler());
        RxJavaPlugins.initIoScheduler(TestScheduler::new);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> new TestScheduler());

        RxJavaPlugins.setInitComputationSchedulerHandler(schedulerCallable -> new TestScheduler());
        RxJavaPlugins.setInitNewThreadSchedulerHandler(schedulerCallable -> new TestScheduler());
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }
}
