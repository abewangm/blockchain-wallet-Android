package piuk.blockchain.android.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.crashlytics.android.answers.ContentViewEvent;

import piuk.blockchain.android.data.answers.Logging;

public abstract class BaseFragment<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
        extends Fragment {

    private PRESENTER presenter;
    private boolean logged;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = createPresenter();
        presenter.initView(getMvpView());
    }

    @CallSuper
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        /* Ensure that pages are only logged as being seen if they are actually visible, and only
         once. This is important for fragments in ViewPagers where they might be instantiated, but
         not actually visible or being accessed. For example: Swipe to receive. */
        if (menuVisible && !logged) {
            logged = true;
            Logging.INSTANCE.logContentView(new ContentViewEvent()
                    .putContentName(getClass().getSimpleName()));
        }
        super.setMenuVisibility(menuVisible);
    }

    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        presenter.onViewPaused();
    }

    @CallSuper
    @Override
    public void onDestroy() {
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

    protected abstract VIEW getMvpView();

}
