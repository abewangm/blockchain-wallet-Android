package piuk.blockchain.android.ui.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;

public abstract class BaseDialogFragment<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
        extends AppCompatDialogFragment {

    private PRESENTER presenter;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = createPresenter();
        presenter.initView(getMvpView());
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
