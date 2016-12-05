package piuk.blockchain.android.data.services;

import info.blockchain.wallet.metadata.MetadataShared;
import info.blockchain.wallet.metadata.data.Contact;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;
import info.blockchain.wallet.metadata.data.Trusted;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class SharedMetaDataService {

    private MetadataShared metadata;

    public SharedMetaDataService(MetadataShared metadata) {
        this.metadata = metadata;
    }

    /**
     * Sets the node for the metadata service. The service will crash without it. Can return and
     * error which will need to be handled.
     *
     * @param deterministicKey A {@link DeterministicKey}, see {@link info.blockchain.wallet.payload.PayloadManager#getMasterKey()}
     */
    public Completable setMetadataNode(DeterministicKey deterministicKey) {
        return Completable.fromCallable(() -> {
            metadata.setMetadataNode(deterministicKey);
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // ACCESS TOKEN
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Generates a signed JSON web token from a nonce using an ECKey
     *
     * @return A signed web token in JSON format
     */
    public Observable<String> getToken() {
        return Observable.fromCallable(() -> metadata.getToken());
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link Trusted} object containing a list of trusted users
     *
     * @param token A signed web token in JSON format
     * @return A {@link Trusted} object
     */
    public Observable<Trusted> getTrustedList(String token) {
        return Observable.fromCallable(() -> metadata.getTrustedList(token));
    }

    /**
     * Check if a contact is trusted or not
     *
     * @param token A signed web token in JSON format
     * @param mdid  The MDID of the user you wish to check
     * @return True if the user is trusted
     */
    public Observable<Boolean> getIfTrusted(String token, String mdid) {
        return Observable.fromCallable(() -> metadata.getTrusted(token, mdid));
    }

    /**
     * Add a contact to the trusted user list
     *
     * @param token A signed web token in JSON format
     * @param mdid  The MDID of the user you wish to trust
     * @return True if successful
     */
    public Observable<Boolean> putTrusted(String token, String mdid) {
        return Observable.fromCallable(() -> metadata.putTrusted(token, mdid));
    }

    /**
     * Remove a contact from the list of trusted users
     *
     * @param token A signed web token in JSON format
     * @param mdid  The MDID of the user you wish to delete
     * @return True if successful
     */
    public Observable<Boolean> deleteTrusted(String token, String mdid) {
        return Observable.fromCallable(() -> metadata.deleteTrusted(token, mdid));
    }

    ///////////////////////////////////////////////////////////////////////////
    // SHARING SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param token       A signed web token in JSON format
     * @param contactInfo The user's contact information
     * @return An {@link Invitation} object
     */
    public Observable<Invitation> createInvitation(String token, Contact contactInfo) {
        return Observable.fromCallable(() -> metadata.createInvitation(token, contactInfo));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param token        A signed web token in JSON format
     * @param invitationId An invitation ID
     * @return An {@link Invitation} object
     */
    public Observable<Invitation> acceptInvitation(String token, String invitationId) {
        return Observable.fromCallable(() -> metadata.acceptInvitation(token, invitationId));
    }

    /**
     * Gets the MDID of a user from an invitation ID, stored in {@link Invitation#getContact()}.
     * getContact() will be null if the recipient hasn't yet accepted the invitataion and revealed
     * their MDID
     *
     * @param token        A signed web token in JSON format
     * @param invitationId An invitation ID
     * @return A {@link Invitation} object
     */
    public Observable<Invitation> readInvitation(String token, String invitationId) {
        return Observable.fromCallable(() -> metadata.readInvitation(token, invitationId));
    }

    /**
     * Deletes an invite from another user
     *
     * @param token        A signed web token in JSON format
     * @param invitationId An invitation ID
     * @return True is successful
     */
    public Observable<Boolean> deleteInvitation(String token, String invitationId) {
        return Observable.fromCallable(() -> metadata.deleteInvitation(token, invitationId));
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUEST SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sends a payment request to a user in the trusted contacts list
     *
     * @param token          A signed web token in JSON format
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     * @return A {@link Message} object
     */
    public Observable<Message> sendPaymentRequest(String token, String recipientMdid, PaymentRequest paymentRequest) {
        return Observable.fromCallable(() -> metadata.sendPaymentRequest(token, recipientMdid, paymentRequest));
    }

    /**
     * Accepts a payment request from a user and optionally adds a note to the transaction
     *
     * @param token          A signed web token in JSON format
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     * @param note           An optional note for the transaction
     * @param receiveAddress The address which you wish to user to receive bitcoin
     * @return A {@link Message} object
     */
    public Observable<Message> acceptPaymentRequest(String token, String recipientMdid, PaymentRequest paymentRequest, String note, String receiveAddress) {
        return Observable.fromCallable(() -> metadata.acceptPaymentRequest(token, recipientMdid, paymentRequest, note, receiveAddress));
    }

    /**
     * Returns a list of payment requests. Optionally, choose to only see requests that are
     * processed
     *
     * @param token         A signed web token in JSON format
     * @param onlyProcessed If true, returns only processed payment requests
     * @return A list of {@link PaymentRequest} objects
     */
    public Observable<List<PaymentRequest>> getPaymentRequests(String token, boolean onlyProcessed) {
        return Observable.fromCallable(() -> metadata.getPaymentRequests(token, onlyProcessed));
    }

    /**
     * Returns a list of payment request responses, ie whether or not another user has paid you.
     * Optionally, choose to only see requests that are processed
     *
     * @param token         A signed web token in JSON format
     * @param onlyProcessed If true, returns only processed payment requests
     * @return A list of {@link PaymentRequestResponse} objects
     */
    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(String token, boolean onlyProcessed) {
        return Observable.fromCallable(() -> metadata.getPaymentRequestResponses(token, onlyProcessed));
    }
}
