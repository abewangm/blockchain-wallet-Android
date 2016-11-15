package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.metadata.data.Share;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import piuk.blockchain.android.data.metadata.TokenMemoryStore;
import piuk.blockchain.android.data.metadata.TokenWebStore;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.SharedMetaDataService;
import piuk.blockchain.android.injection.Injector;

@SuppressWarnings("WeakerAccess")
public class MetaDataManager {

    @Inject protected SharedMetaDataService sharedMetaDataService;
    @Inject protected PayloadManager payloadManager;
    @Inject protected TokenWebStore webStore;
    @Inject protected TokenMemoryStore memoryStore;

    {
        Injector.getInstance().getMetaDataComponent().inject(this);
    }

    private <T> Observable<T> callWithToken(TokenRequest<T> function) {

        TokenFunction<T> tokenRequiredFunction = new TokenFunction<T>() {
            @Override
            public Observable<T> apply(String accessToken) {
                return function.apply(accessToken);
            }
        };

        // Attempt to get token from memory then from web, return first entry
        return Observable.defer(() -> Observable.concat(memoryStore.getToken(), webStore.getToken())
                // Ensure token isn't empty
                .filter(s -> !s.isEmpty())
                // Store the token
                .doOnNext(token -> memoryStore.store(token))
                // Invalidate the token if an error occurs
                .doOnError(throwable -> memoryStore.invalidate())
                // Retry on error to get a new token and try the call again
                .retry(1)
                // Emit first item
                .firstElement()
                // Finally call original function and pass the token
                .flatMapObservable(tokenRequiredFunction));
    }

    /**
     * Interface to wrap {@link #callWithToken(TokenRequest)} to allow collapsing into lambda
     */
    interface TokenRequest<T> {
        Observable<T> apply(String accessToken);
    }

    abstract static class TokenFunction<T> implements Function<String, Observable<T>> {
        public abstract Observable<T> apply(String accessToken);
    }

    /**
     * Creates a one time UUID for pairing with another user
     *
     * @return A {@link Share} object with an id for sharing
     */
    public Observable<Share> postShare() {
        return callWithToken(accessToken -> sharedMetaDataService.postShare(accessToken))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Takes another user's UUID gets their MDID
     *
     * @param uuid The user's UUID
     * @return A {@link Share} object with an MDID
     */
    public Observable<Share> postToShare(String uuid) {
        return callWithToken(accessToken -> sharedMetaDataService.postToShare(accessToken, uuid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Adds another user's MDID to your trusted list
     *
     * @param mdid The user's MDID
     * @return True if successful
     */
    public Observable<Boolean> putTrusted(String mdid) {
        return callWithToken(accessToken -> sharedMetaDataService.putTrusted(accessToken, mdid))
                .compose(RxUtil.applySchedulersToObservable());
    }

}
