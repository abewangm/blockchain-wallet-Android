package piuk.blockchain.android.ui.contacts.payments

import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.ContactsDataManager
import piuk.blockchain.android.data.contacts.ContactsPredicates
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.Companion.ARGUMENT_CONFIRMATION_DETAILS
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.Companion.ARGUMENT_CONTACT_ID
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.Companion.ARGUMENT_SATOSHIS
import piuk.blockchain.android.ui.customviews.ToastCustom
import javax.inject.Inject

class ContactsPaymentRequestPresenter @Inject internal constructor(
        private val contactsDataManager: ContactsDataManager
) : BasePresenter<ContactPaymentRequestView>() {

    @VisibleForTesting internal var recipient: Contact? = null
    @VisibleForTesting internal var satoshis: Int? = null

    override fun onViewReady() {
        val fragmentBundle = view.fragmentBundle
        val contactId = fragmentBundle.getString(ARGUMENT_CONTACT_ID)
        val confirmationDetails: PaymentConfirmationDetails =
                fragmentBundle.getParcelable(ARGUMENT_CONFIRMATION_DETAILS)
        satoshis = fragmentBundle.getInt(ARGUMENT_SATOSHIS)

        @Suppress("SENSELESS_COMPARISON")
        if (contactId != null && confirmationDetails != null && satoshis != null) {
            loadContact(contactId)
            updateUi(confirmationDetails)
        } else {
            throw IllegalArgumentException("Contact ID, confirmation details and satoshi amount must be passed to fragment")
        }
    }

    internal fun sendRequest() {
        view.showProgressDialog()
        val request = RequestForPaymentRequest(satoshis!!.toLong(), view.note)
        // Request that the other person receives payment
        contactsDataManager.requestReceivePayment(recipient!!.mdid, request)
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doAfterTerminate { view.dismissProgressDialog() }
                .subscribe(
                        { view.showRequestSuccessfulDialog() },
                        {
                            view.showToast(
                                    R.string.contacts_error_sending_payment_request,
                                    ToastCustom.TYPE_ERROR
                            )
                        })
    }

    private fun updateUi(confirmationDetails: PaymentConfirmationDetails) {
        view.updateAccountName(confirmationDetails.fromLabel)
        view.updateTotalBtc("${confirmationDetails.btcTotal} ${confirmationDetails.btcUnit}")
        view.updateTotalFiat(confirmationDetails.fiatSymbol + confirmationDetails.fiatTotal)
    }

    private fun loadContact(contactId: String) {
        contactsDataManager.getContactList()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .filter(ContactsPredicates.filterById(contactId))
                .subscribe(
                        { contact ->
                            recipient = contact
                            view.contactLoaded(recipient!!.name)
                        },
                        {
                            view.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR)
                            view.finishPage()
                        },
                        {
                            if (recipient == null) {
                                // Wasn't found via filter, show not found
                                view.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR)
                                view.finishPage()
                            }
                        })
    }

}
