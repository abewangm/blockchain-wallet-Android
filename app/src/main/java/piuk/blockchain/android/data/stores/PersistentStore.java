package piuk.blockchain.android.data.stores;

import io.reactivex.Observable;

public interface PersistentStore<T> {

    Observable<T> store(T data);

    void invalidate();

}
