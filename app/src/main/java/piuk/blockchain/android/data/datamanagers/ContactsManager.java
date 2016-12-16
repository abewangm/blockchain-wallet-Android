package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;

import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.ContactsService;

@SuppressWarnings("WeakerAccess")
public class ContactsManager {

//    @Inject
    protected ContactsService contactsService;

    // TODO: 15/12/2016
//    {
//        Injector.getInstance().getContactsComponent().inject(this);
//    }

    public ContactsManager(ContactsService contactsService) {
        this.contactsService = contactsService;
    }


    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails       My details that will be visible in invitation url
     * @param recipientDetails Recipient details - This will be added to my contacts list
     * @return A {@link Contact} object
     */
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {

        return contactsService.createInvitation(myDetails, recipientDetails)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationUrl An invitation url
     * @return An {@link Invitation} object
     */
    public Observable<Contact> acceptInvitation(String invitationUrl) {
        return contactsService.acceptInvitation(invitationUrl).compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<List<Contact>> getContactList() {
        return contactsService.getContactList().compose(RxUtil.applySchedulersToObservable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUESTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sends a payment request to a user in the trusted contactsService list
     *
     * @param recipientMdid  The MDID of the message's recipient
     * @param paymentRequest A PaymentRequest object containing information about the proposed
     *                       transaction
     */
    public void sendPaymentRequest(String recipientMdid, PaymentRequest paymentRequest) throws Exception {
        // TODO: 15/12/2016 catch exception
        contactsService.sendPaymentRequest(recipientMdid, paymentRequest);
//                .compose(RxUtil.applySchedulersToObservable());
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
        return contactsService.acceptPaymentRequest(recipientMdid, paymentRequest, note, receiveAddress)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of payment requests. Optionally, choose to only see requests that are
     * processed
     *
     * @return A list of {@link PaymentRequest} objects
     */
    public Observable<List<PaymentRequest>> getPaymentRequests() {
        return contactsService.getPaymentRequests()
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of payment request responses, ie whether or not another user has paid you.
     * Optionally, choose to only see requests that are processed
     *
     * @param onlyNew If true, returns only new payment requests
     * @return A list of {@link PaymentRequestResponse} objects
     */
    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(boolean onlyNew) {
        return contactsService.getPaymentRequestResponses(onlyNew)
                .compose(RxUtil.applySchedulersToObservable());
    }

}
