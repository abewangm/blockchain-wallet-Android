package piuk.blockchain.android.data.contacts

import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.contacts.data.PaymentRequest
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import info.blockchain.wallet.metadata.data.Message
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.android.data.contacts.datastore.ContactsMapStore
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.stores.PendingTransactionListStore
import piuk.blockchain.android.util.annotations.Mockable
import java.util.*

/**
 * A manager for handling all Metadata/Shared Metadata/Contacts based operations. Using this class
 * requires careful initialisation, which should be done as follows:
 *
 * 1) Load the metadata nodes from the metadata service using [PayloadDataManager.loadNodes]. This
 * will return false if the nodes cannot be found.
 *
 * 2) Generate nodes if necessary. If step 1 returns false, the nodes must be generated using
 * [PayloadDataManager.generateNodes]. In theory, this means that the nodes only need to be
 * generated once, and thus users with a second password only need to be prompted to enter their
 * password once.
 *
 * 3) Init the Contacts Service using [initContactsService], passing in the appropriate nodes loaded
 * by [PayloadDataManager.loadNodes].
 *
 * 4) Register the user's derived MDID with the Shared Metadata service using
 * [PayloadDataManager.registerMdid].
 *
 * 5) Finally, publish the user's XPub to the Shared Metadata service via [publishXpub].
 */
@Mockable
class ContactsDataManager(
        private val contactsService: ContactsService,
        private val contactsMapStore: ContactsMapStore,
        private val pendingTransactionListStore: PendingTransactionListStore,
        rxBus: RxBus
) {

    private val rxPinning: RxPinning = RxPinning(rxBus)

    /**
     * Initialises the Contacts service.
     *
     * @param metadataNode       A [DeterministicKey] representing the Metadata Node
     * @param sharedMetadataNode A [DeterministicKey] representing the Shared Metadata node
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun initContactsService(metadataNode: DeterministicKey, sharedMetadataNode: DeterministicKey): Completable {
        return rxPinning.call { contactsService.initContactsService(metadataNode, sharedMetadataNode) }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Invalidates the access token for re-authing, if needed.
     */
    private fun invalidate(): Completable {
        return rxPinning.call { contactsService.invalidate() }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONTACTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Fetches an updated version of the contacts list and parses [FacilitatedTransaction]
     * objects into a map if completed.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun fetchContacts(): Completable {
        return rxPinning.call { contactsService.fetchContacts() }
                .andThen(contactsService.getContactList())
                .doOnNext { contact ->
                    for (tx in contact.facilitatedTransactions.values) {
                        if (tx.txHash != null && !tx.txHash.isEmpty()) {
                            contactsMapStore.contactsTransactionMap.put(tx.txHash, contact.name)
                            if (tx.note != null && !tx.note.isEmpty()) {
                                contactsMapStore.notesTransactionMap.put(tx.txHash, tx.note)
                            }
                        }
                    }
                }
                .toList()
                .toCompletable()
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Saves the contacts list that's currently in memory to the metadata endpoint
     *
     * @return A [Completable] object, ie an asynchronous void operation≈≈
     */
    fun saveContacts(): Completable {
        return rxPinning.call { contactsService.saveContacts() }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Completely wipes your contact list from the metadata endpoint. Does not update memory.
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun wipeContacts(): Completable {
        return rxPinning.call { contactsService.wipeContacts() }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Returns a stream of [Contact] objects, comprising a list of users. List can be empty.
     *
     * @return A stream of [Contact] objects
     */
    fun getContactList(): Observable<Contact> = contactsService.getContactList()
            .compose(RxUtil.applySchedulersToObservable())

    /**
     * Returns a stream of [Contact] objects, comprising of a list of users with [ ] objects that
     * need responding to.
     *
     * @return A stream of [Contact] objects
     */
    fun getContactsWithUnreadPaymentRequests(): Observable<Contact> =
            callWithToken(contactsService.getContactsWithUnreadPaymentRequests())
                    .compose(RxUtil.applySchedulersToObservable())

    /**
     * Inserts a contact into the locally stored Contacts list. Saves this list to server.
     *
     * @param contact The [Contact] to be stored
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun addContact(contact: Contact): Completable {
        return rxPinning.call { contactsService.addContact(contact) }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Removes a contact from the locally stored Contacts list. Saves updated list to server.
     *
     * @param contact The [Contact] to be stored
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun removeContact(contact: Contact): Completable {
        return rxPinning.call { contactsService.removeContact(contact) }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Renames a [Contact] and then saves the changes to the server.
     *
     * @param contactId The ID of the Contact you wish to update
     * @param name      The new name for the Contact
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun renameContact(contactId: String, name: String): Completable {
        return rxPinning.call { contactsService.renameContact(contactId, name) }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // INVITATIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new invite and associated invite ID for linking two users together
     *
     * @param myDetails        My details that will be visible in invitation url
     * @param recipientDetails Recipient details
     *
     * @return A [Contact] object, which is an updated version of the mydetails object, ie
     * it's the sender's own contact details
     */
    fun createInvitation(myDetails: Contact, recipientDetails: Contact): Observable<Contact> {
        return callWithToken(contactsService.createInvitation(myDetails, recipientDetails))
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Accepts an invitation from another user
     *
     * @param invitationUrl An invitation url
     *
     * @return A [Contact] object representing the other user
     */
    fun acceptInvitation(invitationUrl: String): Observable<Contact> {
        return callWithToken(contactsService.acceptInvitation(invitationUrl))
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns some Contact information from an invitation link
     *
     * @param url The URL which has been sent to the user
     *
     * @return An [Observable] wrapping a Contact
     */
    fun readInvitationLink(url: String): Observable<Contact> {
        return callWithToken(contactsService.readInvitationLink(url))
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Allows the user to poll to check if the passed Contact has accepted their invite
     *
     * @param contact The [Contact] to be queried
     *
     * @return An [Observable] wrapping a boolean value, returning true if the invitation has
     * been accepted
     */
    fun readInvitationSent(contact: Contact): Observable<Boolean> {
        return callWithToken(contactsService.readInvitationSent(contact))
                .compose(RxUtil.applySchedulersToObservable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // PAYMENT REQUESTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Requests that another user sends you a payment
     *
     * @param mdid    The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note
     *
     * @return A [Completable] object
     */
    fun requestSendPayment(mdid: String, request: PaymentRequest): Completable {
        return callWithToken(contactsService.requestSendPayment(mdid, request))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Requests that another user receive bitcoin from current user
     *
     * @param mdid    The recipient's MDID
     * @param request A [PaymentRequest] object containing the request details, ie the amount
     *                and an optional note, the receive address
     *
     * @return A [Completable] object
     */
    fun requestReceivePayment(mdid: String, request: RequestForPaymentRequest): Completable {
        return callWithToken(contactsService.requestReceivePayment(mdid, request))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Sends a response to a payment request containing a [PaymentRequest], which contains a
     * bitcoin address belonging to the user.
     *
     * @param mdid            The recipient's MDID*
     * @param paymentRequest  A [PaymentRequest] object
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    fun sendPaymentRequestResponse(mdid: String, paymentRequest: PaymentRequest, facilitatedTxId: String): Completable {
        return callWithToken(contactsService.sendPaymentRequestResponse(mdid, paymentRequest, facilitatedTxId))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Sends notification that a transaction has been processed.
     *
     * @param mdid            The recipient's MDID
     * @param txHash          The transaction hash
     * @param facilitatedTxId The ID of the [FacilitatedTransaction]
     *
     * @return A [Completable] object
     */
    fun sendPaymentBroadcasted(mdid: String, txHash: String, facilitatedTxId: String): Completable {
        return callWithToken(contactsService.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Sends a response to a payment request declining the offer of payment.
     *
     * @param mdid   The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be declined
     *
     * @return A [Completable] object
     */
    fun sendPaymentDeclinedResponse(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.sendPaymentDeclinedResponse(mdid, fctxId))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Informs the recipient of a payment request that the request has been cancelled.
     *
     * @param mdid   The recipient's MDID
     * @param fctxId The ID of the [FacilitatedTransaction] to be cancelled
     *
     * @return A [Completable] object
     */
    fun sendPaymentCancelledResponse(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.sendPaymentCancelledResponse(mdid, fctxId))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // XPUB AND MDID HANDLING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the XPub associated with an MDID, should the user already be in your trusted contacts
     * list
     *
     * @param mdid The MDID of the user you wish to query
     *
     * @return A [Observable] wrapping a String
     */
    fun fetchXpub(mdid: String): Observable<String> {
        return rxPinning.call<String> { contactsService.fetchXpub(mdid) }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Publishes the user's XPub to the metadata service
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun publishXpub(): Completable {
        return rxPinning.call { contactsService.publishXpub() }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // MESSAGES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns a list of [Message] objects, with a flag to only return those which haven't
     * been read yet. Can return an empty list.
     *
     * @param onlyNew If true, returns only the unread messages
     *
     * @return An [Observable] wrapping a list of Message objects
     */
    fun getMessages(onlyNew: Boolean): Observable<List<Message>> {
        return callWithToken(contactsService.getMessages(onlyNew))
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Allows users to read a particular message by retrieving it from the Shared Metadata service
     *
     * @param messageId The ID of the message to be read
     *
     * @return An [Observable] wrapping a [Message]
     */
    fun readMessage(messageId: String): Observable<Message> {
        return callWithToken(contactsService.readMessage(messageId))
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Marks a message as read or unread
     *
     * @param messageId  The ID of the message to be marked as read/unread
     * @param markAsRead A flag setting the read status
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun markMessageAsRead(messageId: String, markAsRead: Boolean): Completable {
        return callWithToken(contactsService.markMessageAsRead(messageId, markAsRead))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    ///////////////////////////////////////////////////////////////////////////
    // FACILITATED TRANSACTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Finds and returns a stream of [ContactTransactionModel] objects and stores them locally
     * where the transaction is yet to be completed, ie the hash is empty. Intended to be used to
     * display a list of transactions with another user in the balance page, and therefore this list
     * does not contain completed, cancelled or declined transactions.

     * @return An [Observable] stream of [ContactTransactionModel] objects
     */
    fun refreshFacilitatedTransactions(): Observable<ContactTransactionModel> {
        pendingTransactionListStore.clearList()
        return getContactList()
                .flatMapIterable { contact ->
                    val transactions = ArrayList<ContactTransactionModel>()
                    for (it in contact.facilitatedTransactions.values) {
                        // If hash is null, transaction has not been completed
                        if ((it.txHash == null || it.txHash.isEmpty())
                                // Filter out cancelled and declined transactions
                                && it.state != FacilitatedTransaction.STATE_CANCELLED
                                && it.state != FacilitatedTransaction.STATE_DECLINED) {

                            val model = ContactTransactionModel(contact.name, it)
                            pendingTransactionListStore.insertTransaction(model)
                            transactions.add(model)
                        }
                    }
                    return@flatMapIterable transactions
                }
    }

    /**
     * Returns a stream of [ContactTransactionModel] objects from disk where the transaction
     * is yet to be completed, ie the hash is empty.
     *
     * @return An [Observable] stream of [ContactTransactionModel] objects
     */
    fun getFacilitatedTransactions(): Observable<ContactTransactionModel> =
            Observable.fromIterable(pendingTransactionListStore.list)

    /**
     * Returns a [Contact] object from a given FacilitatedTransaction ID. It's possible that
     * the Observable will return an empty object, but very unlikely.
     *
     * @param fctxId The [FacilitatedTransaction] ID.
     *
     * @return A [Single] emitting a [Contact] object or will emit a [ ] if the Contact isn't found.
     */
    fun getContactFromFctxId(fctxId: String): Single<Contact> = getContactList()
            .filter { it.facilitatedTransactions[fctxId] != null }
            .firstOrError()

    /**
     * Deletes a [FacilitatedTransaction] object from a [Contact] and then syncs the
     * Contact list with the server.
     *
     * @param mdid   The Contact's MDID
     *
     * @param fctxId The FacilitatedTransaction's ID
     *
     * @return A [Completable] object, ie an asynchronous void operation
     */
    fun deleteFacilitatedTransaction(mdid: String, fctxId: String): Completable {
        return callWithToken(contactsService.deleteFacilitatedTransaction(mdid, fctxId))
                .compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Returns a Map of Contact names keyed to transaction hashes.
     *
     * @return A [HashMap] where the key is a [TransactionSummary.getHash], and the
     * value is a [Contact.getName]
     */
    fun getContactsTransactionMap() = contactsMapStore.contactsTransactionMap

    /**
     * Returns a Map of [FacilitatedTransaction] notes keyed to Transaction hashes.
     *
     * @return A [HashMap] where the key is a [TransactionSummary.getHash], and the
     * value is a [FacilitatedTransaction.getNote]
     */
    fun getNotesTransactionMap() = contactsMapStore.notesTransactionMap

    /**
     * Clears all data in the [PendingTransactionListStore].
     */
    fun resetContacts() {
        contactsService.destroy()
        pendingTransactionListStore.clearList()
        contactsMapStore.clearContactsTransactionMap()
        contactsMapStore.clearNotesTransactionMap()
    }

    ///////////////////////////////////////////////////////////////////////////
    // TOKEN FUNCTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls a function and invalidates the access token on failure before calling the original
     * function again, which will trigger getting another access token. Called via [RxPinning]
     * which propagates an error to the UI when SSL pinning fails.
     */
    private fun <T> callWithToken(observable: Observable<T>): Observable<T> {
        return rxPinning.call<T> { getRetry(observable) }
    }

    /**
     * Calls a function and invalidates the access token on failure before calling the original
     * function again, which will trigger getting another access token. Called via [RxPinning]
     * which propagates an error to the UI when SSL pinning fails.
     */
    private fun callWithToken(completable: Completable): Completable {
        return rxPinning.call { getRetry(completable) }
    }

    private fun <T> getRetry(observable: Observable<T>): Observable<T> {
        return Observable.defer<T> { observable }
                .doOnError { invalidate() }
                .retry(1)
    }

    private fun getRetry(completable: Completable): Completable {
        return Completable.defer { completable }
                .doOnError { invalidate() }
                .retry(1)
    }

}
