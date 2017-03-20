package piuk.blockchain.android.data.rxjava;

import java.io.IOException;

import javax.net.ssl.SSLPeerUnverifiedException;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import piuk.blockchain.android.data.connectivity.ConnectionEvent;

@SuppressWarnings("AnonymousInnerClassMayBeStatic")
public class RxPinning {

    private RxBus rxBus;

    public RxPinning(RxBus rxBus) {
        this.rxBus = rxBus;
        rxBus.register(ConnectionEvent.class);
    }

    /**
     * Wraps an {@link Observable} and calls it, handling any errors and emitting {@link
     * ConnectionEvent} objects in response if necessary. Specifically, this method handles SSL
     * pinning issues and network I/O problems.
     *
     * Please note that this is not necessary for calls which don't hit the network, as this method
     * interprets {@link IOException} errors as connectivity issues, which will frustrate/confuse
     * the user.
     *
     * @param function An {@link Observable} function
     * @param <T>      The {@link Observable} type
     * @return A wrapped {@link Observable}
     */
    public <T> Observable<T> callObservable(ObservableNetworkRequest<T> function) {
        ObservableNetworkFunction<T> tokenFunction = new ObservableNetworkFunction<T>() {
            @Override
            public Observable<T> apply(Void empty) {
                return function.apply();
            }
        };

        return Observable.defer(() -> tokenFunction.apply(null))
                .doOnError(this::handleError);
    }

    /**
     * Wraps a {@link Completable} and calls it, handling any errors and emitting {@link
     * ConnectionEvent} objects in response if necessary. Specifically, this method handles SSL
     * pinning issues and network I/O problems.
     *
     * Please note that this is not necessary for calls which don't hit the network, as this method
     * interprets {@link IOException} errors as connectivity issues, which will frustrate/confuse
     * the user.
     *
     * @param function A {@link Completable} function
     * @return A wrapped {@link Completable}
     */
    public Completable callCompletable(CompletableNetworkRequest function) {
        CompletableNetworkFunction tokenFunction = new CompletableNetworkFunction() {
            @Override
            public Completable apply(Void empty) {
                return function.apply();
            }
        };

        return Completable.defer(() -> tokenFunction.apply(null))
                .doOnError(this::handleError);
    }

    /**
     * Checks the supplied {@link Throwable} and emits a specific {@link ConnectionEvent} as
     * appropriate.
     *
     * @param throwable A {@link} Throwable emitted by a webcall of some description
     */
    private void handleError(Throwable throwable) {
        if (throwable instanceof SSLPeerUnverifiedException) {
            rxBus.emitEvent(ConnectionEvent.class, ConnectionEvent.PINNING_FAIL);
        } else if (throwable instanceof IOException) {
            rxBus.emitEvent(ConnectionEvent.class, ConnectionEvent.NO_CONNECTION);
        }
    }

    // For collapsing into Lambdas
    public interface ObservableNetworkRequest<T> {
        Observable<T> apply();
    }

    // For collapsing into Lambdas
    public interface CompletableNetworkRequest {
        Completable apply();
    }

    abstract static class ObservableNetworkFunction<T> implements Function<Void, Observable<T>> {
        public abstract Observable<T> apply(Void empty);
    }

    abstract static class CompletableNetworkFunction implements Function<Void, Completable> {
        public abstract Completable apply(Void empty);
    }

}
