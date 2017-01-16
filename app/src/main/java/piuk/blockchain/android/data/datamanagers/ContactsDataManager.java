package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.core.ECKey;

import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.ContactsService;

@SuppressWarnings({"WeakerAccess", "AnonymousInnerClassMayBeStatic"})
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
                .andThen(registerMdid(
                                payloadManager.getPayload().getGuid(),
                                payloadManager.getPayload().getSharedKey(),
                                payloadManager.getMasterKey()))
                .andThen(publishXpub())
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
     * Registers the user's MDID with the metadata service
     *
     * @param guid      The user's GUID
     * @param sharedKey The user's shared key
     * @param node      The user's wallet's master key
     * @return A {@link Completable}, ie an Observable type object specifically for methods
     * returning void.
     */
    public Completable registerMdid(String guid, String sharedKey, ECKey node) {
        return Completable.fromCallable(() -> {
            payloadManager.registerMdid(guid, sharedKey, node);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Unregisters the user's MDID with the metadata service
     *
     * @param guid      The user's GUID
     * @param sharedKey The user's shared key
     * @param node      The user's wallet's master key
     * @return A {@link Completable}, ie an Observable type object specifically for methods
     * returning void.
     */
    public Completable unregisterMdid(String guid, String sharedKey, ECKey node) {
        return Completable.fromCallable(() -> {
            payloadManager.unregisterMdid(guid, sharedKey, node);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Invalidates the access token for re-authing
     */
    private Completable invalidate() {
        return contactsService.invalidate()
                .compose(RxUtil.applySchedulersToCompletable());
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
    private interface ObservableTokenRequest<T> {
        Observable<T> apply();
    }

    // For collapsing into Lambdas
    private interface CompletableTokenRequest {
        Completable apply();
    }

    abstract static class ObservableTokenFunction<T> implements Function<Void, Observable<T>> {
        public abstract Observable<T> apply(Void empty);
    }

    abstract static class CompletableTokenFunction implements Function<Void, Completable> {
        public abstract Completable apply(Void empty);
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches an updated version of the contacts list
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable fetchContacts() {
        return contactsService.fetchContacts()
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Saves the contacts list that's currently in memory to the metadata endpoint
     *
     * @return A {@link Completable} object, ie an asynchronous void operation≈≈
     */
    public Completable saveContacts() {
        return contactsService.saveContacts()
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Completely wipes your contact list from the metadata endpoint. Does not update memory.
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable wipeContacts() {
        return contactsService.wipeContacts()
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a stream of {@link Contact} objects, comprising a list of users. List can be empty.
     *
     * @return A stream of {@link Contact} objects
     */
    public Observable<Contact> getContactList() {
        return contactsService.getContactList()
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Inserts a contact into the locally stored Contacts list. Saves this list to server.
     *
     * @param contact The {@link Contact} to be stored
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable addContact(Contact contact) {
        return contactsService.addContact(contact)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Removes a contact from the locally stored Contacts list. Saves updated list to server.
     *
     * @param contact The {@link Contact} to be stored
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable removeContact(Contact contact) {
        return contactsService.removeContact(contact)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details
     * @return A {@link Contact} object, which is an updated version of the mydetails object, ie
     * it's the sender's own contact details
     */
    public Observable<Contact> createInvitation(Contact myDetails, Contact recipientDetails) {
        return callWithToken(() -> contactsService.createInvitation(myDetails, recipientDetails))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationUrl An invitation url
     * @return A {@link Contact} object representing the other user
     */
    public Observable<Contact> acceptInvitation(String invitationUrl) {
        return callWithToken(() -> contactsService.acceptInvitation(invitationUrl))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns some Contact information from an invitation link
     *
     * @param url The URL which has been sent to the user
     * @return An {@link Observable} wrapping a Contact
     */
    public Observable<Contact> readInvitationLink(String url) {
        return callWithToken(() -> contactsService.readInvitationLink(url))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Allows the user to poll to check if the passed Contact has accepted their invite
     *
     * @param contact The {@link Contact} to be queried
     * @return An {@link Observable} wrapping a boolean value, returning true if the invitation has
     * been accepted
     */
    public Observable<Boolean> readInvitationSent(Contact contact) {
        return callWithToken(() -> contactsService.readInvitationSent(contact))
                .compose(RxUtil.applySchedulersToObservable());
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUESTS
    ///////////////////////////////////////////////////////////////////////////

//    /**
//     * Sends a payment request to a user in the trusted contactsService list
//     *
//     * @param recipientMdid The MDID of the message's recipient
//     * @param satoshis      The number of satoshis to be requested
//     */
//    public Completable sendPaymentRequest(String recipientMdid, long satoshis) throws Exception {
//        return callWithToken(() -> contactsService.sendPaymentRequest(recipientMdid, satoshis))
//                .compose(RxUtil.applySchedulersToCompletable());
//    }

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
//    public Observable<Message> acceptPaymentRequest(String recipientMdid, PaymentRequest paymentRequest, String note, String receiveAddress) {
//        return callWithToken(() -> contactsService.acceptPaymentRequest(recipientMdid, paymentRequest, note, receiveAddress))
//                .compose(RxUtil.applySchedulersToObservable());
//    }
//
//    /**
//     * Returns a list of payment requests. Optionally, choose to only see requests that are
//     * processed
//     *
//     * @return A list of {@link PaymentRequest} objects
//     */
//    public Observable<List<PaymentRequest>> getPaymentRequests() {
//        return callWithToken(() -> contactsService.getPaymentRequests())
//                .compose(RxUtil.applySchedulersToObservable());
//    }
//
//    /**
//     * Returns a list of payment request responses, ie whether or not another user has paid you.
//     * Optionally, choose to only see requests that are processed
//     *
//     * @param onlyNew If true, returns only new payment requests
//     * @return A list of {@link PaymentRequestResponse} objects
//     */
//    public Observable<List<PaymentRequestResponse>> getPaymentRequestResponses(boolean onlyNew) {
//        return callWithToken(() -> contactsService.getPaymentRequestResponses(onlyNew)
//                .compose(RxUtil.applySchedulersToObservable()));
//    }

    ///////////////////////////////////////////////////////////////////////////
    // XPUB AND MDID HANDLING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the XPub associated with an MDID, should the user already be in your trusted contacts
     * list
     *
     * @param mdid The MDID of the user you wish to query
     * @return A {@link Observable} wrapping a String
     */
    public Observable<String> fetchXpub(String mdid) {
        return contactsService.fetchXpub(mdid)
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Publishes the user's XPub to the metadata service
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable publishXpub() {
        return contactsService.publishXpub()
                .compose(RxUtil.applySchedulersToCompletable());
    }

//    /**
//     * Adds a user's MDID to the trusted list in Shared Metadata
//     *
//     * @param mdid The user's MDID
//     * @return An {@link Observable} wrapping a boolean, representing a successful save
//     */
//    public Observable<Boolean> addTrusted(String mdid) {
//        return callWithToken(() -> contactsService.addTrusted(mdid))
//                .compose(RxUtil.applySchedulersToObservable());
//    }
//
//    /**
//     * Removes a user's MDID from the trusted list in Shared Metadata
//     *
//     * @param mdid The user's MDID
//     * @return An {@link Observable} wrapping a boolean, representing a successful deletion
//     */
//    public Observable<Boolean> deleteTrusted(String mdid) {
//        return callWithToken(() -> contactsService.deleteTrusted(mdid))
//                .compose(RxUtil.applySchedulersToObservable());
//    }

    ///////////////////////////////////////////////////////////////////////////
    // MESSAGES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of {@link Message} objects, with a flag to only return those which haven't
     * been read yet. Can return an empty list.
     *
     * @param onlyNew If true, returns only the unread messages
     * @return An {@link Observable} wrapping a list of Message objects
     */
    public Observable<List<Message>> getMessages(boolean onlyNew) {
        return callWithToken(() -> contactsService.getMessages(onlyNew))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Allows users to read a particular message by retrieving it from the Shared Metadata service
     *
     * @param messageId The ID of the message to be read
     * @return An {@link Observable} wrapping a {@link Message}
     */
    public Observable<Message> readMessage(String messageId) {
        return callWithToken(() -> contactsService.readMessage(messageId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Marks a message as read or unread
     *
     * @param messageId  The ID of the message to be marked as read/unread
     * @param markAsRead A flag setting the read status
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable markMessageAsRead(String messageId, boolean markAsRead) {
        return callWithToken(() -> contactsService.markMessageAsRead(messageId, markAsRead))
                .compose(RxUtil.applySchedulersToCompletable());
    }

//    /**
//     * Decrypts a message from a specific user
//     *
//     * @param message The string to be decrypted
//     * @param mdid    The MDID of the user who sent the message
//     * @return An {@link Observable} containing the decrypted message
//     */
//    public Observable<Message> decryptMessageFrom(Message message, String mdid) {
//        return callWithToken(() -> contactsService.decryptMessageFrom(message, mdid))
//                .compose(RxUtil.applySchedulersToObservable());
//    }

}
