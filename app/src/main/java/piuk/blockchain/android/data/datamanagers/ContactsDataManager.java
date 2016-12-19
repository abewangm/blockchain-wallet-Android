package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.metadata.data.Invitation;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.PaymentRequestResponse;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.ContactsService;

@SuppressWarnings("WeakerAccess")
public class ContactsDataManager {

    private ContactsService contactsService;
    private PayloadManager payloadManager;

    public ContactsDataManager(ContactsService contactsService, PayloadManager payloadManager) {
        this.contactsService = contactsService;
        this.payloadManager = payloadManager;
    }

    /**
     * Initialises the Contacts service
     *
     * @param secondPassword The user's second password, if applicable
     * @return A {@link Completable} object
     */
    public Completable initContactsService(@Nullable String secondPassword) {
        return getNodeFactory(secondPassword)
                .flatMapCompletable(metadataNodeFactory -> contactsService.initContactsService(
                        metadataNodeFactory.getMetadataNode(),
                        metadataNodeFactory.getSharedMetadataNode()))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    private Observable<MetadataNodeFactory> getNodeFactory(String secondPassword) {
        return Observable.fromCallable(() -> {
            payloadManager.loadNodes(
                    payloadManager.getPayload().getGuid(),
                    payloadManager.getPayload().getSharedKey(),
                    payloadManager.getTempPassword().toString(),
                    secondPassword);
            return payloadManager.getMetadataNodeFactory();
        });
    }

    /**
     * Calls a function and invalidates the access token on failure before calling the original
     * function again, which will trigger getting another access token.
     */
    private <T> Observable<T> callWithToken(ObservableTokenRequest<T> function) {
        ObservableTokenFunction<T> tokenFunction = new ObservableTokenFunction<T>() {

            @Override
            public Observable<T> apply(Void empty) {
                return function.apply();
            }
        };

        return Observable.defer(() -> tokenFunction.apply(null))
                .doOnError(throwable -> invalidate())
                .retry(1);
    }

    private Completable callWithToken(CompletableTokenRequest function) {
        CompletableTokenFunction tokenFunction = new CompletableTokenFunction() {

            @Override
            public Completable apply(Void aVoid) {
                return function.apply();
            }
        };

        return Completable.defer(() -> tokenFunction.apply(null))
                .doOnError(throwable -> invalidate())
                .retry(1);
    }

    // For collapsing into Lambdas
    interface ObservableTokenRequest<T> {
        Observable<T> apply();
    }

    interface CompletableTokenRequest {
        Completable apply();
    }

    abstract static class ObservableTokenFunction<T> implements Function<Void, Observable<T>> {
        public abstract Observable<T> apply(Void empty);
    }

    abstract static class CompletableTokenFunction implements Function<Void, Completable> {
        public abstract Completable apply(Void empty);
    }

    /**
     * Invalidates the access token for re-authing
     */
    private Completable invalidate() {
        return contactsService.invalidate()
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details - This will be added to my contacts list
     * @return A {@link Contact} object
     */
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {
        return callWithToken(() -> contactsService.createInvitation(myDetails, recipientDetails)
                .compose(RxUtil.applySchedulersToObservable()));
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationUrl An invitation url
     * @return An {@link Invitation} object
     */
    public Observable<Contact> acceptInvitation(String invitationUrl) {
        return callWithToken(() -> contactsService.acceptInvitation(invitationUrl)
                .compose(RxUtil.applySchedulersToObservable()));
    }

    /**
     * Fetches an updated version of the contacts list
     *
     * @return A Completable object, ie an asynchronous void operation
     */
    public Completable fetchContacts() {
        return contactsService.fetchContacts()
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a list of Contact objects, stored in memory in the Contacts object
     *
     * @return An Observable wrapping a list of {@link Contact} objects
     */
    public Observable<List<Contact>> getContactList() {
        return contactsService.getContactList();
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
    public Completable sendPaymentRequest(String recipientMdid, PaymentRequest paymentRequest) throws Exception {
        return callWithToken(() -> contactsService.sendPaymentRequest(recipientMdid, paymentRequest))
                .compose(RxUtil.applySchedulersToCompletable());
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
        return callWithToken(() -> contactsService.acceptPaymentRequest(recipientMdid, paymentRequest, note, receiveAddress))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of payment requests. Optionally, choose to only see requests that are
     * processed
     *
     * @return A list of {@link PaymentRequest} objects
     */
    public Observable<List<PaymentRequest>> getPaymentRequests() {
        return callWithToken(() -> contactsService.getPaymentRequests())
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
        return callWithToken(() -> contactsService.getPaymentRequestResponses(onlyNew)
                .compose(RxUtil.applySchedulersToObservable()));
    }

}
