package piuk.blockchain.android.data.services;

import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

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
     * Returns a {@link List<Contact>} object containing a list of trusted users
     *
     * @return A {@link List<Contact>} object
     */
    public Observable<List<Contact>> getContactList() {
        return Observable.just(contacts.getContactList());
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
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {
        return Observable.fromCallable(() -> contacts.createInvitation(myDetails, recipientDetails));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param url An invitation url
     * @return An {@link Invitation} object
     */
    public Observable<Contact> acceptInvitation(String url) {
        return Observable.fromCallable(() -> contacts.acceptInvitationLink(url));
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUEST SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sends a payment request to a user in the trusted contactsService list
     *
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     */
    public Completable sendPaymentRequest(String recipientMdid, PaymentRequest paymentRequest) {
        return Completable.fromCallable(() -> {
            contacts.sendPaymentRequest(recipientMdid, paymentRequest);
            return Void.TYPE;
        });
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
        return Observable.fromCallable(() -> contacts.acceptPaymentRequest(recipientMdid, paymentRequest, note, receiveAddress));
    }

    /**
     * Returns a list of payment requests. Optionally, choose to only see requests that are
     * processed
     *
     * @return A list of {@link PaymentRequest} objects
     */
    public Observable<List<PaymentRequest>> getPaymentRequests() {
        return Observable.fromCallable(() -> contacts.getPaymentRequests());
    }

    /**
     * Returns a list of payment request responses, ie whether or not another user has paid you.
     * Optionally, choose to only see requests that are processed
     *
     * @param onlyNew If true, returns only new payment requests
     * @return A list of {@link PaymentRequestResponse} objects
     */
    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(boolean onlyNew) {
        return Observable.fromCallable(() -> contacts.getPaymentRequestResponses(onlyNew));
    }
}
