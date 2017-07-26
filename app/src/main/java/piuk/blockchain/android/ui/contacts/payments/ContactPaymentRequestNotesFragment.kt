package piuk.blockchain.android.ui.contacts.payments

import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_contact_payment_request_notes.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.getTextString
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible
import javax.inject.Inject

class ContactPaymentRequestNotesFragment : BaseFragment<ContactPaymentRequestView, ContactsPaymentRequestPresenter>(),
        ContactPaymentRequestView {

    override val fragmentBundle: Bundle
        get() = arguments
    override val note: String
        get() = edittext_description.getTextString()

    @Inject lateinit var paymentRequestPresenter: ContactsPaymentRequestPresenter
    private var progressDialog: MaterialProgressDialog? = null
    private var listener: FragmentInteractionListener? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_contact_payment_request_notes)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_send.setOnClickListener {
            presenter.sendRequest()
            ViewUtils.hideKeyboard(activity)
        }

//        setupToolbar()
        onViewReady()
    }

    override fun contactLoaded(name: String) {
        textview_to_name.text = name
        edittext_description.hint = getString(R.string.contacts_confirm_shared_with, name)
        loading_layout.gone()
        main_layout.visible()
    }

    override fun updateAccountName(name: String) {
        textview_from_address.text = name
    }

    override fun updateTotalBtc(total: String) {
        textview_total_btc.text = total
    }

    override fun updateTotalFiat(total: String) {
        textview_total_fiat.text = total
    }

    override fun finishPage() {
        listener?.onPageFinished()
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(context)
                .apply {
                    setCancelable(false)
                    setMessage(R.string.please_wait)
                    show()
                }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(context, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun showRequestSuccessfulDialog() {
        // TODO: Remove me and show success fragment instead by triggering callback
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(getString(R.string.contacts_payment_success_waiting_title))
                .setMessage(R.string.contacts_payment_success_waiting_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> finishPage() }
                .show()
    }

    override fun createPresenter() = paymentRequestPresenter

    override fun getMvpView() = this

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement FragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun setupToolbar() {
        // TODO: For now this isn't reset when leaving this page  
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.contacts_confirm_title)
        } else {
            finishPage()
        }
    }

    interface FragmentInteractionListener {

        fun onPageFinished()

    }

    companion object {

        const val ARGUMENT_CONFIRMATION_DETAILS = "ARGUMENT_CONFIRMATION_DETAILS"
        const val ARGUMENT_CONTACT_ID = "ARGUMENT_CONTACT_ID"
        const val ARGUMENT_SATOSHIS = "ARGUMENT_SATOSHIS"

        @JvmStatic
        fun newInstance(
                confirmationDetails: PaymentConfirmationDetails,
                contactId: String,
                satoshis: Int
        ): ContactPaymentRequestNotesFragment {

            val args = Bundle().apply {
                putParcelable(ARGUMENT_CONFIRMATION_DETAILS, confirmationDetails)
                putString(ARGUMENT_CONTACT_ID, contactId)
                putInt(ARGUMENT_SATOSHIS, satoshis)
            }
            return ContactPaymentRequestNotesFragment().apply { arguments = args }
        }
    }

}
