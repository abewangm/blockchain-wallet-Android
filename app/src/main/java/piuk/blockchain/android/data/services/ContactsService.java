package piuk.blockchain.android.data.services;

import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
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
     * Returns a stream of {@link Contact} objects, comprising a list of users. List can be empty.
     *
     * @return A stream of {@link Contact} objects
     */
    public Observable<Contact> getContactList() {
        return Observable.defer(() -> Observable.fromIterable(contacts.getContactList().values()));
    }

    /**
     * Returns a stream of {@link Contact} objects, comprising of a list of users with {@link
     * FacilitatedTransaction} objects that need responding to.
     *
     * @return A stream of {@link Contact} objects
     */
    @RequiresAccessToken
    public Observable<Contact> getContactsWithUnreadPaymentRequests() {
        return Observable.defer(() -> Observable.fromIterable(contacts.digestUnreadPaymentRequests()));
    }

    /**
     * Inserts a contact into the locally stored Contacts list. Saves this list to server.
     *
     * @param contact The {@link Contact} to be stored
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable addContact(Contact contact) {
        return Completable.fromAction(() -> contacts.addContact(contact));
    }

    /**
     * Removes a contact from the locally stored Contacts list. Saves updated list to server
     */
    public Completable removeContact(Contact contact) {
        return Completable.fromAction(() -> contacts.removeContact(contact));
    }

    ///////////////////////////////////////////////////////////////////////////
    // SHARING SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details
     * @return A {@link Contact} object, which is an updated version of the mydetails object, ie
     * it's the sender's own contact details
     */
    @RequiresAccessToken
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {
        return Observable.fromCallable(() -> contacts.createInvitation(myDetails, recipientDetails));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param url An invitation url
     * @return A {@link Contact} object containing the other user
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
     * Sends a new payment request without the need to ask for a receive address from the recipient.
     *
     * @param mdid    The recipient's MDID
     * @param request A {@link PaymentRequest} object containing the request details, ie the amount
     *                and an optional note
     * @return A {@link Completable} object
     */
    @RequiresAccessToken
    public Completable requestSendPayment(String mdid, PaymentRequest request) {
        return Completable.fromCallable(() -> {
            contacts.sendPaymentRequest(mdid, request);
            return Void.TYPE;
        });
    }

    /**
     * Requests that another user receive bitcoin from current user
     *
     * @param mdid    The recipient's MDID
     * @param request A {@link PaymentRequest} object containing the request details, ie the amount
     *                and an optional note, the receive address
     * @return A {@link Completable} object
     */
    @RequiresAccessToken
    public Completable requestReceivePayment(String mdid, RequestForPaymentRequest request) {
        return Completable.fromCallable(() -> {
            contacts.sendRequestForPaymentRequest(mdid, request);
            return Void.TYPE;
        });
    }

    /**
     * Sends a response to a payment request.
     *
     * @param mdid            The recipient's MDID
     * @param paymentRequest  A {@link PaymentRequest} object
     * @param facilitatedTxId The ID of the {@link FacilitatedTransaction}
     * @return A {@link Completable} object
     */
    @RequiresAccessToken
    public Completable sendPaymentRequestResponse(String mdid, PaymentRequest paymentRequest, String facilitatedTxId) {
        return Completable.fromCallable(() -> {
            contacts.sendPaymentRequest(mdid, paymentRequest, facilitatedTxId);
            return Void.TYPE;
        });
    }

    /**
     * Sends notification that a transaction has been processed.
     *
     * @param mdid            The recipient's MDID
     * @param txHash          The transaction hash
     * @param facilitatedTxId The ID of the {@link FacilitatedTransaction}
     * @return A {@link Completable} object
     */
    @RequiresAccessToken
    public Completable sendPaymentBroadcasted(String mdid, String txHash, String facilitatedTxId) {
        return Completable.fromCallable(() -> {
            contacts.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId);
            return Void.TYPE;
        });
    }

    /**
     * Compares a broadcasted payment with the expected payment value.Returns an int which
     * represents whether the sender of the transaction sent too much, too little or the correct
     * amount of bitcoin. Can call onError with an IOException if the network is down, for instance
     *
     * @param facilitatedTransaction A {@link FacilitatedTransaction} object
     * @param txHash                 The hash of the transaction
     * @return Returns -1 if too little bitcoin was sent, 1 if too much was sent and 0 if the value
     * was correct
     */
    public Observable<Integer> checkPaymentValue(FacilitatedTransaction facilitatedTransaction, String txHash) {
        return Observable.fromCallable(() -> contacts.checkPaymentValue(facilitatedTransaction, txHash));
    }

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

}
