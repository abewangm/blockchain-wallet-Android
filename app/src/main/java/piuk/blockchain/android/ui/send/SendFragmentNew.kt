package piuk.blockchain.android.ui.send

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
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
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PermissionUtil
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import javax.inject.Inject

class SendFragmentNew : BaseFragment<SendViewNew, SendPresenterNew>(), SendViewNew {

    @Inject lateinit var sendPresenterNew: SendPresenterNew

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

        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        setTabs()
        setupInitialAccount()
        setupSendingView()
        setupReceivingView()
        setupFeesView()
        button_send.setOnClickListener { onSendClicked() }
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

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun createPresenter() = sendPresenterNew

    override fun getMvpView() = this

    private fun setTabs() {
        tabs.apply {
            addTab(tabs.newTab().setText(R.string.bitcoin))
            addTab(tabs.newTab().setText(R.string.ether))
            setOnTabSelectedListener {
                if (it == 0) {
                    presenter.onBitcoinChosen()
                } else {
                    presenter.onEtherChosen()
                }
            }
        }
    }

    private fun setupToolbar() {
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.send_bitcoin)
        } else {
            finishPage(false)
        }
    }

    override fun finishPage(paymentMade: Boolean) {

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.getItemId()) {
            R.id.action_qr -> {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    PermissionUtil.requestCameraPermissionFromFragment(view!!.rootView, this)
                } else {
                    startScanActivity(SCAN_URI)
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun startScanActivity(code: Int) {
        if (!AppUtil(activity).isCameraOpen) {
            val intent = Intent(activity, CaptureActivity::class.java)
            startActivityForResult(intent, code)
        } else {
            showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanActivity(SCAN_URI)
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

        toContainer.toArrow.setOnClickListener({ v ->
            AccountChooserActivity.startForResult(this,
                    AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND,
                    PaymentRequestType.SEND,
                    getString(R.string.to))
        })
    }

    private fun setupInitialAccount() {

        if (arguments != null) {
            presenter.selectSendingBtcAccount(arguments.getInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, -1))
        } else {
            presenter.selectDefaultSendingAccount()
        }
    }

    private fun setupSendingView() {
        fromContainer.fromAddressTextView.setOnClickListener({ v -> startFromFragment() })
        fromContainer.fromArrowImage.setOnClickListener({ v -> startFromFragment() })
    }

    override fun setSendingAddress(accountItem: ItemAccount) {
        fromContainer.fromAddressTextView.setText(accountItem.label)
    }

    override fun setReceivingHint(hint: Int) {
        toContainer.toAddressEditTextView.setHint(hint)
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

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(activity, getString(message), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun showSendingFieldDropdown() {
        fromContainer.fromArrowImage.visible()
    }

    override fun hideSendingFieldDropdown() {
        fromContainer.fromArrowImage.gone()
    }

    override fun showReceivingDropdown() {
        toContainer.toArrow.visible()
    }

    override fun hideReceivingDropdown() {
        toContainer.toArrow.gone()
    }

    private fun setupFeesView() {
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

        val SCAN_URI = 2010
        val SCAN_PRIVX = 2011

        fun newInstance(scanData: String?,
                        scanRoute: String?,
                        selectedAccountPosition: Int): SendFragmentNew {
            val fragment = SendFragmentNew()
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
                        fctxId: String): SendFragmentNew {
            val fragment = SendFragmentNew()
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