package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.metadata.data.Message;
import info.blockchain.api.metadata.data.Share;
import info.blockchain.api.metadata.data.Trusted;
import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.core.ECKey;

import java.util.List;

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
                // Emit first item
                .firstElement()
                // Call original function and pass the token
                .flatMapObservable(tokenRequiredFunction)
                // Invalidate the token if an error occurs
                .doOnError(throwable -> memoryStore.invalidate())
                // Retry on error to get a new token and try the call again
                .retry(1));
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
     * Takes another user's UUID and gets their MDID
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

    /**
     * Deletes a specified contact from the trusted list
     *
     * @param mdid The MDID you wish to delete
     * @return True is delete operation was successful
     */
    public Observable<Boolean> deleteTrusted(String mdid) {
        return callWithToken(accessToken -> sharedMetaDataService.deleteTrusted(accessToken, mdid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns the user's list of trusted users
     *
     * @return A {@link Trusted} object containing a String array of contacts
     */
    public Observable<Trusted> getTrustedList() {
        return callWithToken(accessToken -> sharedMetaDataService.getTrustedList(accessToken))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Adds a message to the shared metadata service. Signed.
     *
     * @param ecKey         A user generated ECKey
     * @param recipientMdid The MDID of the message's recipient
     * @param message       The message itself in plaintext
     * @param type          An integer message type todo define these somewhere
     * @return A {@link Message} object
     */
    public Observable<Message> postMessage(ECKey ecKey, String recipientMdid, String message, int type) {
        return callWithToken(accessToken -> sharedMetaDataService.postMessage(accessToken, ecKey, recipientMdid, message, type))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of the user's messages. Optionally return only those which are processed
     *
     * @param onlyProcessed Flag for only getting processed messages
     * @return A list of {@link Message} objects
     */
    public Observable<List<Message>> getMessages(boolean onlyProcessed) {
        return callWithToken(accessToken -> sharedMetaDataService.getMessages(accessToken, onlyProcessed))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Get a specific message using a message ID
     *
     * @param messageId The ID of the message to be retrieved
     * @return A {@link Message} object
     */
    public Observable<Message> getMessageForId(String messageId) {
        return callWithToken(accessToken -> sharedMetaDataService.getMessageForId(accessToken, messageId));
    }

}
