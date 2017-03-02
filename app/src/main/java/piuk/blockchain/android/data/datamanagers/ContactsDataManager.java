package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.metadata.data.Message;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.transaction.Tx;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import piuk.blockchain.android.data.contacts.ContactTransactionModel;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.ContactsService;
import piuk.blockchain.android.data.stores.PendingTransactionListStore;

/**
 * A manager for handling all Metadata/Shared Metadata/Contacts based operations. Using this class
 * requires careful initialisation, which should be done as follows:
 *
 * 1) Load the metadata nodes from the metadata service using {@link this#loadNodes()}. This will
 * return false if the nodes cannot be found.
 *
 * 2) Generate nodes if necessary. If step 1 returns false, the nodes must be generated using {@link
 * this#generateNodes(String)}. In theory, this means that the nodes only need to be generated once,
 * and thus users with a second password only need to be prompted to enter their password once.
 *
 * 3) Init the Contacts Service using {@link this#initContactsService(DeterministicKey,
 * DeterministicKey)}, passing in the appropriate nodes loaded by {@link this#loadNodes()}.
 *
 * 4) Register the user's derived MDID with the Shared Metadata service using {@link
 * this#registerMdid()}.
 *
 * 5) Finally, publish the user's XPub to the Shared Metadata service via {@link this#publishXpub()}
 */
@SuppressWarnings({"WeakerAccess", "AnonymousInnerClassMayBeStatic"})
public class ContactsDataManager {

    private ContactsService contactsService;
    private PayloadManager payloadManager;
    private PendingTransactionListStore pendingTransactionListStore;
    @VisibleForTesting HashMap<String, String> contactsTransactionMap = new HashMap<>();

    public ContactsDataManager(ContactsService contactsService,
                               PayloadManager payloadManager,
                               PendingTransactionListStore pendingTransactionListStore) {
        this.contactsService = contactsService;
        this.payloadManager = payloadManager;
        this.pendingTransactionListStore = pendingTransactionListStore;
    }

    /**
     * Loads previously saved nodes from the Metadata service. If none are found, the {@link
     * Observable} returns false.
     *
     * @return An {@link Observable} object wrapping a boolean value, representing successfully
     * loaded nodes
     */
    public Observable<Boolean> loadNodes() {
        return Observable.fromCallable(() -> payloadManager.loadNodes(
                payloadManager.getPayload().getGuid(),
                payloadManager.getPayload().getSharedKey(),
                payloadManager.getTempPassword().toString()))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Generates the metadata and shared metadata nodes if necessary.
     *
     * @param secondPassword An optional second password.
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable generateNodes(@Nullable String secondPassword) {
        return Completable.fromCallable(() -> {
            payloadManager.generateNodes(secondPassword);
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Initialises the Contacts service.
     *
     * @param metadataNode       A {@link DeterministicKey} representing the Metadata Node
     * @param sharedMetadataNode A {@link DeterministicKey} representing the Shared Metadata node
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable initContactsService(DeterministicKey metadataNode, DeterministicKey sharedMetadataNode) {
        return contactsService.initContactsService(metadataNode, sharedMetadataNode)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a {@link MetadataNodeFactory} object which allows you to access the {@link
     * DeterministicKey} objects needed to initialise the Contacts service.
     *
     * @return An {@link Observable} wrapping a {@link MetadataNodeFactory}
     */
    public Observable<MetadataNodeFactory> getMetadataNodeFactory() {
        return Observable.just(payloadManager.getMetadataNodeFactory());
    }

    /**
     * Registers the user's MDID with the metadata service.
     *
     * @return A {@link Completable}, ie an Observable type object specifically for methods
     * returning void.
     */
    public Completable registerMdid() {
        return Completable.fromCallable(() -> {
            payloadManager.registerMdid(
                    payloadManager.getPayload().getGuid(),
                    payloadManager.getPayload().getSharedKey(),
                    payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Unregisters the user's MDID from the metadata service.
     *
     * @return A {@link Completable}, ie an Observable type object specifically for methods
     * returning void.
     */
    public Completable unregisterMdid() {
        return Completable.fromCallable(() -> {
            payloadManager.unregisterMdid(
                    payloadManager.getPayload().getGuid(),
                    payloadManager.getPayload().getSharedKey(),
                    payloadManager.getMetadataNodeFactory().getSharedMetadataNode());
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Invalidates the access token for re-authing, if needed.
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
     * Fetches an updated version of the contacts list and parses {@link FacilitatedTransaction}
     * objects into a map if completed.
     *
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    @SuppressWarnings("Convert2streamapi")
    public Completable fetchContacts() {
        return contactsService.fetchContacts()
                .andThen(contactsService.getContactList())
                .toList()
                .doOnEvent((contacts, throwable) -> {
                    contactsTransactionMap.clear();
                    if (contacts != null) {
                        for (Contact contact : contacts) {
                            for (FacilitatedTransaction tx : contact.getFacilitatedTransactions().values()) {
                                if (tx.getTxHash() != null && !tx.getTxHash().isEmpty()) {
                                    contactsTransactionMap.put(tx.getTxHash(), contact.getName());
                                }
                            }
                        }
                    }
                })
                .toCompletable()
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
     * Returns a stream of {@link Contact} objects, comprising of a list of users with {@link
     * FacilitatedTransaction} objects that need responding to.
     *
     * @return A stream of {@link Contact} objects
     */
    public Observable<Contact> getContactsWithUnreadPaymentRequests() {
        return callWithToken(() -> contactsService.getContactsWithUnreadPaymentRequests())
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

    /**
     * Renames a {@link Contact} and then saves the changes to the server.
     *
     * @param contactId The ID of the Contact you wish to update
     * @param name      The new name for the Contact
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable renameContact(String contactId, String name) {
        return contactsService.renameContact(contactId, name)
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

    /**
     * Requests that another user sends you a payment
     *
     * @param mdid    The recipient's MDID
     * @param request A {@link PaymentRequest} object containing the request details, ie the amount
     *                and an optional note
     * @return A {@link Completable} object
     */
    public Completable requestSendPayment(String mdid, PaymentRequest request) {
        return callWithToken(() -> contactsService.requestSendPayment(mdid, request))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Requests that another user receive bitcoin from current user
     *
     * @param mdid    The recipient's MDID
     * @param request A {@link PaymentRequest} object containing the request details, ie the amount
     *                and an optional note, the receive address
     * @return A {@link Completable} object
     */
    public Completable requestReceivePayment(String mdid, RequestForPaymentRequest request) {
        return callWithToken(() -> contactsService.requestReceivePayment(mdid, request))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Sends a response to a payment request.
     *
     * @param mdid            The recipient's MDID
     * @param paymentRequest  A {@link PaymentRequest} object
     * @param facilitatedTxId The ID of the {@link FacilitatedTransaction}
     * @return A {@link Completable} object
     */
    public Completable sendPaymentRequestResponse(String mdid, PaymentRequest paymentRequest, String facilitatedTxId) {
        return callWithToken(() -> contactsService.sendPaymentRequestResponse(mdid, paymentRequest, facilitatedTxId))
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Sends notification that a transaction has been processed.
     *
     * @param mdid            The recipient's MDID
     * @param txHash          The transaction hash
     * @param facilitatedTxId The ID of the {@link FacilitatedTransaction}
     * @return A {@link Completable} object
     */
    public Completable sendPaymentBroadcasted(String mdid, String txHash, String facilitatedTxId) {
        return callWithToken(() -> contactsService.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId))
                .compose(RxUtil.applySchedulersToCompletable());
    }

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

    ///////////////////////////////////////////////////////////////////////////
    // FACILITATED TRANSACTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Finds and returns a stream of {@link ContactTransactionModel} objects and stores them locally
     * where the transaction is yet to be completed, ie the hash is empty.
     *
     * @return An {@link Observable} stream of {@link ContactTransactionModel} objects
     */
    @SuppressWarnings("Convert2streamapi")
    public Observable<ContactTransactionModel> refreshFacilitatedTransactions() {
        pendingTransactionListStore.clearList();
        return getContactList()
                .toList()
                .toObservable()
                .flatMap(contacts -> {
                    ArrayList<ContactTransactionModel> transactions = new ArrayList<>();
                    for (Contact contact : contacts) {
                        for (FacilitatedTransaction transaction : contact.getFacilitatedTransactions().values()) {
                            // If hash is null, transaction has not been completed
                            if (transaction.getTxHash() == null || transaction.getTxHash().isEmpty()) {
                                ContactTransactionModel model = new ContactTransactionModel(contact.getName(), transaction);
                                pendingTransactionListStore.insertTransaction(model);
                                transactions.add(model);
                            }
                        }
                    }
                    return Observable.fromIterable(transactions);
                });
    }

    /**
     * Returns a stream of {@link ContactTransactionModel} objects from disk where the transaction
     * is yet to be completed, ie the hash is empty.
     *
     * @return An {@link Observable} stream of {@link ContactTransactionModel} objects
     */
    public Observable<ContactTransactionModel> getFacilitatedTransactions() {
        return Observable.fromIterable(pendingTransactionListStore.getList());
    }

    /**
     * Returns a {@link Contact} object from a given FacilitatedTransaction ID. It's possible that
     * the Observable will return an empty object, but very unlikely.
     *
     * @param fctxId The {@link FacilitatedTransaction} ID.
     * @return An {@link Observable} containing a {@link Contact} object OR potentially an empty
     * Observable
     */
    public Observable<Contact> getContactFromFctxId(String fctxId) {
        return getContactList()
                .toList()
                .toObservable()
                .flatMap(contacts -> {
                    for (Contact contact : contacts) {
                        if (contact.getFacilitatedTransactions().get(fctxId) != null) {
                            return Observable.just(contact);
                        }
                    }

                    return Observable.empty();
                });
    }

    /**
     * Deletes a {@link FacilitatedTransaction} object from a {@link Contact} and then syncs the
     * Contact list with the server.
     *
     * @param mdid   The Contact's MDID
     * @param fctxId The FacilitatedTransaction's ID
     * @return A {@link Completable} object, ie an asynchronous void operation
     */
    public Completable deleteFacilitatedTransaction(String mdid, String fctxId) {
        return contactsService.deleteFacilitatedTransaction(mdid, fctxId)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a Map of Contact names keyed to transaction hashes.
     *
     * @return A {@link HashMap} where the key is a {@link Tx#getHash()}, and the value is a {@link
     * Contact#getName()}
     */
    public HashMap<String, String> getContactsTransactionMap() {
        return contactsTransactionMap;
    }
}
