package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.metadata.data.Contact;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;
import info.blockchain.wallet.metadata.data.Trusted;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import piuk.blockchain.android.data.metadata.TokenMemoryStore;
import piuk.blockchain.android.data.metadata.TokenWebStore;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.SharedMetadataService;
import piuk.blockchain.android.injection.Injector;

@SuppressWarnings("WeakerAccess")
public class SharedMetadataManager {

    @Inject protected SharedMetadataService sharedMetaDataService;
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
     * Sets the node for the metadata service. The service will crash without it. Can return and
     * error which will need to be handled.
     *
     * @param deterministicKey A {@link DeterministicKey}, see {@link info.blockchain.wallet.payload.PayloadManager#getMasterKey()}
     */
    public Completable setMetadataNode(DeterministicKey deterministicKey) {
        return sharedMetaDataService.setMetadataNode(deterministicKey)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param contactInfo The contact info of the invite sender
     * @return An {@link Invitation} object
     */
    public Observable<Invitation> createInvitation(Contact contactInfo) {
        return callWithToken(accessToken -> sharedMetaDataService.createInvitation(accessToken, contactInfo)
                .compose(RxUtil.applySchedulersToObservable()));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationId An invitation ID
     * @return An {@link Invitation} object
     */
    public Observable<Invitation> acceptInvitation(String invitationId) {
        return callWithToken(accessToken -> sharedMetaDataService.acceptInvitation(accessToken, invitationId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Gets the MDID of a user from an invitation ID, stored in {@link Invitation#getContact()}.
     * getContact() will be null if the recipient hasn't yet accepted the invitataion and revealed
     * their MDID
     *
     * @param invitationId An invitation ID
     * @return A {@link Invitation} object
     */
    public Observable<Invitation> readInvitation(String invitationId) {
        return callWithToken(accessToken -> sharedMetaDataService.readInvitation(accessToken, invitationId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Deletes an invite from another user
     *
     * @param invitationId An invitation ID
     * @return True is successful
     */
    public Observable<Boolean> deleteInvitation(String invitationId) {
        return callWithToken(accessToken -> sharedMetaDataService.deleteInvitation(accessToken, invitationId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // TRUSTED LIST
    ///////////////////////////////////////////////////////////////////////////

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
     * Check if a contact is trusted or not
     *
     * @param mdid The MDID of the user you wish to check
     * @return True if the user is trusted
     */
    public Observable<Boolean> getIfTrusted(String mdid) {
        return callWithToken(accessToken -> sharedMetaDataService.getIfTrusted(accessToken, mdid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Add a contact to the trusted user list
     *
     * @param mdid The MDID of the user you wish to trust
     * @return True if successful
     */
    public Observable<Boolean> putTrusted(String mdid) {
        return callWithToken(accessToken -> sharedMetaDataService.putTrusted(accessToken, mdid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Remove a contact from the list of trusted users
     *
     * @param mdid The MDID of the user you wish to delete
     * @return True if successful
     */
    public Observable<Boolean> deleteTrusted(String mdid) {
        return callWithToken(accessToken -> sharedMetaDataService.deleteTrusted(accessToken, mdid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUESTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sends a payment request to a user in the trusted contacts list
     *
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     * @return A {@link Message} object
     */
    public Observable<Message> sendPaymentRequest(String recipientMdid, PaymentRequest paymentRequest) {
        return callWithToken(accessToken -> sharedMetaDataService.sendPaymentRequest(accessToken, recipientMdid, paymentRequest))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Accepts a payment request from a user and optionally adds a note to the transaction
     *
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     * @param note           An optional note for the transaction
     * @param receiveAddress The address which you wish to user to receive bitcoin
     * @return A {@link Message} object
     */
    public Observable<Message> acceptPaymentRequest(String recipientMdid, PaymentRequest paymentRequest, String note, String receiveAddress) {
        return callWithToken(accessToken -> sharedMetaDataService.acceptPaymentRequest(accessToken, recipientMdid, paymentRequest, note, receiveAddress))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of payment requests. Optionally, choose to only see requests that are
     * processed
     *
     * @param onlyProcessed If true, returns only processed payment requests
     * @return A list of {@link PaymentRequest} objects
     */
    public Observable<List<PaymentRequest>> getPaymentRequests(boolean onlyProcessed) {
        return callWithToken(accessToken -> sharedMetaDataService.getPaymentRequests(accessToken, onlyProcessed))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of payment request responses, ie whether or not another user has paid you.
     * Optionally, choose to only see requests that are processed
     *
     * @param onlyProcessed If true, returns only processed payment requests
     * @return A list of {@link PaymentRequestResponse} objects
     */
    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(boolean onlyProcessed) {
        return callWithToken(accessToken -> sharedMetaDataService.getPaymentRequestResponses(accessToken, onlyProcessed))
                .compose(RxUtil.applySchedulersToObservable());
    }

}
