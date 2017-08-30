package piuk.blockchain.android.ui.send

import android.os.Bundle
import android.view.*
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.include_from_row.view.*
import kotlinx.android.synthetic.main.include_to_row_editable.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import javax.inject.Inject

class SendFragment    : BaseFragment<SendView, SendPresenter>(), SendView {

    @Inject lateinit var sendPresenter: SendPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = container!!.inflate(R.layout.fragment_send)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_send.setOnClickListener { onSendClicked() }

        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        setupSendingView()
        setupReceivingView()

        setTabs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater!!.inflate(R.menu.menu_send, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun createPresenter() = sendPresenter

    override fun getMvpView() = this

    private fun setTabs() {
        tabs.apply {
            addTab(tabs.newTab().setText(R.string.bitcoin))
            addTab(tabs.newTab().setText(R.string.ether))
            setOnTabSelectedListener {
                if (it == 1) {
                    presenter.onBitcoinChosen()
                } else {
                    presenter.onEtherChosen()
                }
            }
        }
    }

    private fun setupReceivingView() {
        //Avoid OntouchListener - causes paste issues on some Samsung devices
        toContainer.toAddressEditTextView.setOnClickListener({ v ->
            toContainer.toAddressEditTextView.setText("")
            presenter.clearReceivingAddress()
        })
        //LongClick listener required to clear receive address in memory when user long clicks to paste
        toContainer.toAddressEditTextView.setOnLongClickListener({ v ->
            toContainer.toAddressEditTextView.setText("")
            presenter.clearReceivingAddress()
            v.performClick()
            false
        })

        //TextChanged listener required to invalidate receive address in memory when user
        //chooses to edit address populated via QR
        RxTextView.textChanges(toContainer.toAddressEditTextView)
                .doOnNext { ignored ->
                    if (activity.currentFocus === toContainer.toAddressEditTextView) {
                        presenter.clearReceivingAddress()
                        presenter.clearContact()
                    }
                }
                .subscribe(IgnorableDefaultObserver())

        toContainer.toArrowImage.setOnClickListener({ v ->
            AccountChooserActivity.startForResult(this,
                    AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND,
                    PaymentRequestType.SEND,
                    getString(R.string.to))
        })
    }

    private fun setupSendingView() {

        if (arguments != null) {
            presenter.selectSendingAccount(arguments.getInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, -1))
        } else {
            presenter.selectDefaultSendingAccount()
        }

        fromContainer.fromAddressTextView.setOnClickListener({ v -> startFromFragment() })
        fromContainer.fromArrowImage.setOnClickListener({ v -> startFromFragment() })
    }

    override fun setSendingAddress(accountItem: ItemAccount) {
        fromContainer.fromAddressTextView.setText(accountItem.label)
    }

    private fun startFromFragment() {
        AccountChooserActivity.startForResult(this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND,
                PaymentRequestType.REQUEST,
                getString(R.string.from))
    }

    fun onChangeFeeClicked() {
    }

    fun onSendClicked() {
        //maybe
        presenter.onContinue()
    }

    fun onBackPressed() {
    }

    interface OnSendFragmentInteractionListener {

        fun onSendFragmentClose(paymentMade: Boolean)

        fun onTransactionNotesRequested(paymentConfirmationDetails: PaymentConfirmationDetails,
                                        paymentRequestType: PaymentRequestType,
                                        contactId: String,
                                        satoshis: Long,
                                        accountPosition: Int)
    }

    companion object {

        val ARGUMENT_SCAN_DATA = "scan_data"
        val ARGUMENT_SELECTED_ACCOUNT_POSITION = "selected_account_position"
        val ARGUMENT_CONTACT_ID = "contact_id"
        val ARGUMENT_CONTACT_MDID = "contact_mdid"
        val ARGUMENT_FCTX_ID = "fctx_id"
        val ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE = "address_input_route"

        fun newInstance(scanData: String?,
                        scanRoute: String?,
                        selectedAccountPosition: Int): SendFragment {
            val fragment = SendFragment()
            val args = Bundle()
            args.putString(ARGUMENT_SCAN_DATA, scanData)
            args.putString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE, scanRoute)
            args.putInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, selectedAccountPosition)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(uri: String,
                        contactId: String,
                        contactMdid: String,
                        fctxId: String): SendFragment {
            val fragment = SendFragment()
            val args = Bundle()
            args.putString(ARGUMENT_SCAN_DATA, uri)
            args.putString(ARGUMENT_CONTACT_ID, contactId)
            args.putString(ARGUMENT_CONTACT_MDID, contactMdid)
            args.putString(ARGUMENT_FCTX_ID, fctxId)
            fragment.arguments = args
            return fragment
        }
    }
}