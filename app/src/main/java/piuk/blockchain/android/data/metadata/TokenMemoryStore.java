package piuk.blockchain.android.data.metadata;

import io.reactivex.Observable;
import piuk.blockchain.android.data.stores.PersistentStore;

public class TokenMemoryStore implements PersistentStore<String>, TokenStore {

    private String token;

    @Override
    public Observable<String> store(String data) {
        token = data;
        return Observable.just(token);
    }

    @Override
    public void invalidate() {
        token = null;
    }

    @Override
    public Observable<String> getToken() {
        return token != null ? Observable.just(token) : Observable.empty();
    }
}
