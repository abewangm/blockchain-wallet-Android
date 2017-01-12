package piuk.blockchain.android.data.services;

import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.util.annotations.RequiresAccessToken;

public class ContactsService {

    private Contacts contacts;

    public ContactsService(Contacts contacts) {
        this.contacts = contacts;
    }

    ///////////////////////////////////////////////////////////////////////////
    // INIT METHODS AND AUTH
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initialises the Contacts service
     *
     * @param metaDataHDNode       The key for the metadata service
     * @param sharedMetaDataHDNode The key for the shared metadata service
     * @return A {@link Completable} object
     */
    public Completable initContactsService(DeterministicKey metaDataHDNode, DeterministicKey sharedMetaDataHDNode) {
        return Completable.fromCallable(() -> {
            contacts.init(metaDataHDNode, sharedMetaDataHDNode);
            return Void.TYPE;
        });
    }

    /**
     * Invalidates the access token for re-authing
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable invalidate() {
        return Completable.fromCallable(() -> {
            contacts.invalidateToken();
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches an updated version of the contacts list
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable fetchContacts() {
        return Completable.fromCallable(() -> {
            contacts.fetch();
            return Void.TYPE;
        });
    }

    /**
     * Saves the contacts list that's currently in memory to the metadata endpoint
     *
     * @return A {@link Completable} object, ie an asynchronous void operation≈≈
     */
    public Completable saveContacts() {
        return Completable.fromCallable(() -> {
            contacts.save();
            return Void.TYPE;
        });
    }

    /**
     * Completely wipes your contact list from the metadata endpoint. Does not update memory.
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable wipeContacts() {
        return Completable.fromCallable(() -> {
            contacts.wipe();
            return Void.TYPE;
        });
    }

    /**
     * Returns a {@link List<Contact>} object containing a list of trusted users. List can be empty.
     *
     * @return A {@link List<Contact>} object
     */
    public Observable<List<Contact>> getContactList() {
        return Observable.just(contacts.getContactList());
    }

    /**
     * Inserts a contact into the locally stored Contacts list. Does not save to server.
     *
     * @param contact The {@link Contact} to be stored
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable addContact(Contact contact) {
        return Completable.fromAction(() -> contacts.addContact(contact));
    }

    ///////////////////////////////////////////////////////////////////////////
    // SHARING SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details - This will be added to my contacts list
     * @return An {@link Invitation} object
     */
    @RequiresAccessToken
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {
        return Observable.fromCallable(() -> contacts.createInvitation(myDetails, recipientDetails));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param url An invitation url
     * @return An {@link Invitation} object
     */
    @RequiresAccessToken
    public Observable<Contact> acceptInvitation(String url) {
        return Observable.fromCallable(() -> contacts.acceptInvitationLink(url));
    }

    /**
     * Returns some Contact information from an invitation link
     *
     * @param url The URL which has been sent to the user
     * @return An {@link Observable} wrapping a Contact
     */
    @RequiresAccessToken
    public Observable<Contact> readInvitationLink(String url) {
        return Observable.fromCallable(() -> contacts.readInvitationLink(url));
    }

    /**
     * Allows the user to poll to check if the passed Contact has accepted their invite
     *
     * @param contact The {@link Contact} to be queried
     * @return An {@link Observable} wrapping a boolean value, returning true if the invitation has
     * been accepted
     */
    @RequiresAccessToken
    public Observable<Boolean> readInvitationSent(Contact contact) {
        return Observable.fromCallable(() -> contacts.readInvitationSent(contact));
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUEST SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sends a payment request to a user in the trusted contactsService list
     *
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A FacilitatedTransaction object containing information about the proposed
     *                       transaction
     */
    @RequiresAccessToken
    public Completable sendPaymentRequest(String recipientMdid, FacilitatedTransaction paymentRequest) {
        return Completable.fromCallable(() -> {
            contacts.sendPaymentRequest(recipientMdid, paymentRequest);
            return Void.TYPE;
        });
    }

//    /**
//     * Accepts a payment request from a user and optionally adds a note to the transaction
//     *
//     * @param recipientMdid  The MDID of the message's recipient
//     * @param paymentRequest A PaymentRequest object containing information about the proposed
//     *                       transaction
//     * @param note           An optional note for the transaction
//     * @param receiveAddress The address which you wish to user to receive bitcoin
//     * @return A {@link Message} object
//     */
//    @RequiresAccessToken
//    public Observable<Message> acceptPaymentRequest(String recipientMdid, PaymentRequest paymentRequest, String note, String receiveAddress) {
//        return Observable.fromCallable(() -> contacts.acceptPaymentRequest(recipientMdid, paymentRequest, note, receiveAddress));
//    }
//
//    /**
//     * Returns a list of payment requests. Optionally, choose to only see requests that are
//     * processed
//     *
//     * @return A list of {@link PaymentRequest} objects
//     */
//    @RequiresAccessToken
//    public Observable<List<PaymentRequest>> getPaymentRequests() {
//        return Observable.fromCallable(() -> contacts.getPaymentRequests());
//    }
//
//    /**
//     * Returns a list of payment request responses, ie whether or not another user has paid you.
//     * Optionally, choose to only see requests that are processed
//     *
//     * @param onlyNew If true, returns only new payment requests
//     * @return A list of {@link PaymentRequestResponse} objects
//     */
//    @RequiresAccessToken
//    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(boolean onlyNew) {
//        return Observable.fromCallable(() -> contacts.getPaymentRequestResponses(onlyNew));
//    }

    ///////////////////////////////////////////////////////////////////////////
    // XPUB AND MDID SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the XPub associated with an MDID, should the user already be in your trusted contacts
     * list
     *
     * @param mdid The MDID of the user you wish to query
     * @return A {@link Observable} wrapping a String
     */
    public Observable<String> fetchXpub(String mdid) {
        return Observable.fromCallable(() -> contacts.fetchXpub(mdid));
    }

    /**
     * Publishes the user's XPub to the metadata service
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable publishXpub() {
        return Completable.fromCallable(() -> {
            contacts.publishXpub();
            return Void.TYPE;
        });
    }

    /**
     * Adds a user's MDID to the trusted list in Shared Metadata
     *
     * @param mdid The user's MDID
     * @return An {@link Observable} wrapping a boolean, representing a successful save
     */
    @RequiresAccessToken
    public Observable<Boolean> addTrusted(String mdid) {
        return Observable.fromCallable(() -> contacts.addTrusted(mdid));
    }

    /**
     * Removes a user's MDID from the trusted list in Shared Metadata
     *
     * @param mdid The user's MDID
     * @return An {@link Observable} wrapping a boolean, representing a successful deletion
     */
    @RequiresAccessToken
    public Observable<Boolean> deleteTrusted(String mdid) {
        return Observable.fromCallable(() -> contacts.deleteTrusted(mdid));
    }

    ///////////////////////////////////////////////////////////////////////////
    // MESSAGES SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of {@link Message} objects, with a flag to only return those which haven't
     * been read yet. Can return an empty list.
     *
     * @param onlyNew If true, returns only the unread messages
     * @return An {@link Observable} wrapping a list of Message objects
     */
    @RequiresAccessToken
    public Observable<List<Message>> getMessages(boolean onlyNew) {
        return Observable.fromCallable(() -> contacts.getMessages(onlyNew));
    }

    /**
     * Allows users to read a particular message by retrieving it from the Shared Metadata service
     *
     * @param messageId The ID of the message to be read
     * @return An {@link Observable} wrapping a {@link Message}
     */
    @RequiresAccessToken
    public Observable<Message> readMessage(String messageId) {
        return Observable.fromCallable(() -> contacts.readMessage(messageId));
    }

    /**
     * Marks a message as read or unread
     *
     * @param messageId  The ID of the message to be marked as read/unread
     * @param markAsRead A flag setting the read status
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    @RequiresAccessToken
    public Completable markMessageAsRead(String messageId, boolean markAsRead) {
        return Completable.fromCallable(() -> {
            contacts.markMessageAsRead(messageId, markAsRead);
            return Void.TYPE;
        });
    }

    /**
     * Decrypts a message from a specific user
     *
     * @param message The string to be decrypted
     * @param mdid    The MDID of the user who sent the message
     * @return An {@link Observable} containing the decrypted message
     */
    @RequiresAccessToken
    public Observable<Message> decryptMessageFrom(Message message, String mdid) {
        return Observable.fromCallable(() -> contacts.decryptMessageFrom(message, mdid));
    }
}
