package piuk.blockchain.android.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import com.crashlytics.android.answers.ContentViewEvent;

import piuk.blockchain.android.data.answers.Logging;

public abstract class BaseMvpActivity<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
        extends BaseAuthActivity {

    private PRESENTER presenter;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = createPresenter();
        presenter.initView(getView());

        logScreenView();
    }

    /**
     * Allows us to disable logging of screen viewing on unimportant pages.
     */
    protected void logScreenView() {
        Logging.INSTANCE.logContentView(new ContentViewEvent()
                .putContentName(getClass().getSimpleName()));
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        presenter.onViewPaused();
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onViewDestroyed();
    }

    protected void onViewReady() {
        presenter.onViewReady();
    }

    protected PRESENTER getPresenter() {
        return presenter;
    }

    protected abstract PRESENTER createPresenter();

    protected abstract VIEW getView();

}
