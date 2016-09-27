package info.blockchain.wallet.viewModel;

import android.support.annotation.CallSuper;
import android.support.annotation.VisibleForTesting;

import rx.subscriptions.CompositeSubscription;

abstract class BaseViewModel {

    @VisibleForTesting CompositeSubscription mCompositeSubscription;

    BaseViewModel() {
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
    }

}
