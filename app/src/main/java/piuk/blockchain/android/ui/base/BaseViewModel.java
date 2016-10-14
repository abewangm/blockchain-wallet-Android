package piuk.blockchain.android.ui.base;

import android.support.annotation.CallSuper;

import piuk.blockchain.android.injection.Injector;
import rx.subscriptions.CompositeSubscription;

public abstract class BaseViewModel {

    public CompositeSubscription mCompositeSubscription;

    public BaseViewModel() {
        mCompositeSubscription = new CompositeSubscription();
    }

    public abstract void onViewReady();

    @CallSuper
    public void destroy() {
        // Clear all subscriptions so that:
        // 1) all processes are cancelled
        // 2) processes don't try to update a null View
        // 3) background processes don't leak memory
        mCompositeSubscription.clear();

        Injector.getInstance().releaseViewModelScope();
    }

}
