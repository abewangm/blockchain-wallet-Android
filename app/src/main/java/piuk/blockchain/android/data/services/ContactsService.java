package piuk.blockchain.android.data.services;

import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;

import java.util.List;

import io.reactivex.Observable;

public class ContactsService {

    private Contacts contacts;

    public ContactsService(Contacts contacts) {
        this.contacts = contacts;
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a {@link List<Contact>} object containing a list of trusted users
     *
     * @return A {@link List<Contact>} object
     */
    public Observable<List<Contact>> getContactList() {

        return Observable.fromCallable(() -> contacts.getContactList());
    }

    ///////////////////////////////////////////////////////////////////////////
    // SHARING SPECIFIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails       My details that will be visible in invitation url
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
    public void sendPaymentRequest(String recipientMdid, PaymentRequest paymentRequest) throws Exception {
        contacts.sendPaymentRequest(recipientMdid, paymentRequest);
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
        // TODO: 15/12/2016 probably no need to catch returned message?
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
