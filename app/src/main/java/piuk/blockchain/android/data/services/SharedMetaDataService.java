package piuk.blockchain.android.data.services;

import info.blockchain.api.metadata.Metadata;
import info.blockchain.api.metadata.data.Message;
import info.blockchain.api.metadata.data.Share;
import info.blockchain.api.metadata.data.Trusted;

import org.bitcoinj.core.ECKey;

import java.util.List;

import io.reactivex.Observable;

public class SharedMetaDataService {

    private Metadata metadata;

    public SharedMetaDataService(Metadata metadata) {
        this.metadata = metadata;
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Generates a signed JSON web token from a nonce using an ECKey
     *
     * @param ecKey A user generated ECKey
     * @return A signed web token in JSON format
     */
    public Observable<String> getToken(ECKey ecKey) {
        return Observable.fromCallable(() -> metadata.getToken(ecKey));
    }

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
     * Obtains a one-time UUID for sharing
     *
     * @param token A signed web token in JSON format
     * @return A {@link Share} object
     */
    public Observable<Share> postShare(String token) {
        return Observable.fromCallable(() -> metadata.postShare(token));
    }

    /**
     * Sets the UUID of the recipient
     *
     * @param token A signed web token in JSON format
     * @param uuid  A UUID
     * @return A {@link Share} object
     */
    public Observable<Share> postToShare(String token, String uuid) {
        return Observable.fromCallable(() -> metadata.postToShare(token, uuid));
    }

    /**
     * Gets the MDID of a sender from one-time UUID
     *
     * @param token A signed web token in JSON format
     * @param uuid  A UUID
     * @return A {@link Share} object
     */
    public Observable<Share> getShare(String token, String uuid) {
        return Observable.fromCallable(() -> metadata.getShare(token, uuid));
    }

    /**
     * Deletes a one-time UUID
     *
     * @param token A signed web token in JSON format
     * @param uuid  A UUID
     * @return True is successful
     */
    public Observable<Boolean> deleteShare(String token, String uuid) {
        return Observable.fromCallable(() -> metadata.deleteShare(token, uuid));
    }

    ///////////////////////////////////////////////////////////////////////////
    // MESSAGES SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a message to the shared metadata service. Signed.
     *
     * @param token         A signed web token in JSON format
     * @param ecKey         A user generated ECKey
     * @param recipientMdid The MDID of the message's recipient
     * @param message       The message itself in plaintext
     * @param type          An integer message type todo define these somewhere
     * @return A {@link Message} object
     */
    public Observable<Message> postMessage(String token, ECKey ecKey, String recipientMdid, String message, int type) {
        return Observable.fromCallable(() -> metadata.postMessage(token, ecKey, recipientMdid, message, type));
    }

    /**
     * Get messages sent to my MDID. Optionally only processed messages
     *
     * @param token         A signed web token in JSON format
     * @param onlyProcessed Flag for only getting processed messages
     * @return A list of {@link Message} objects
     */
    public Observable<List<Message>> getMessages(String token, boolean onlyProcessed) {
        return Observable.fromCallable(() -> metadata.getMessages(token, onlyProcessed));
    }

    /**
     * Get a specific message using a message ID
     *
     * @param token     A signed web token in JSON format
     * @param messageId The ID of the message to be retrieved
     * @return A {@link Message} object
     */
    public Observable<Message> getMessageForId(String token, String messageId) {
        return Observable.fromCallable(() -> metadata.getMessage(token, messageId));
    }

}
