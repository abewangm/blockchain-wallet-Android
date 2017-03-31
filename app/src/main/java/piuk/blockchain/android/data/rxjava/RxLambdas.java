package piuk.blockchain.android.data.rxjava;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

public final class RxLambdas {

    // For collapsing into Lambdas
    public interface ObservableRequest<T> {
        Observable<T> apply();
    }

    // For collapsing into Lambdas
    public interface CompletableRequest {
        Completable apply();
    }

    public abstract static class ObservableFunction<T> implements Function<Void, Observable<T>> {
        public abstract Observable<T> apply(Void empty);
    }

    public abstract static class CompletableFunction implements Function<Void, Completable> {
        public abstract Completable apply(Void empty);
    }

}
