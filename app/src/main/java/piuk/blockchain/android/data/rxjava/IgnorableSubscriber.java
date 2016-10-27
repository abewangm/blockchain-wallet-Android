package piuk.blockchain.android.data.rxjava;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

/**
 * To be used when the result of the subscription can be ignored
 */
public class IgnorableSubscriber<T> extends Subscriber<T> implements Observer<T>, Subscription {

    @Override
    public void onCompleted() {
        // No-op
    }

    @Override
    public void onError(Throwable e) {
        // No-op
    }

    @Override
    public void onNext(Object o) {
        // No-op
    }
}