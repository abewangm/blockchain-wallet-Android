package piuk.blockchain.android.ui.send

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.include_amount_row.*
import kotlinx.android.synthetic.main.include_amount_row.view.*
import kotlinx.android.synthetic.main.include_from_row.view.*
import kotlinx.android.synthetic.main.include_to_row_editable.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
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
import piuk.blockchain.android.util.extensions.invisible
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import timber.log.Timber
import java.util.concurrent.TimeUnit
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

        CurrencyState.getInstance().cryptoCurrency = CryptoCurrencies.BTC
        setTabs()
        setupInitialAccount()
        setupSendingView()
        setupReceivingView()
        setupBtcTextField()
        setupFiatTextField()
        setupFeesView()

        buttonSend.setOnClickListener {
            if (ConnectivityStatus.hasConnectivity(activity)) {
                onSendClicked()
            } else {
                showToast(R.string.check_connectivity_exit, ToastCustom.TYPE_ERROR)
            }
        }
        max.setOnClickListener({ presenter.onSpendAllClicked(getFeePriority()) })
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
        toContainer.toAddressEditTextView.setOnClickListener({
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
                .doOnNext {
                    if (activity.currentFocus === toContainer.toAddressEditTextView) {
                        presenter.clearReceivingAddress()
                        presenter.clearContact()
                    }
                }
                .subscribe(IgnorableDefaultObserver())

        toContainer.toArrow.setOnClickListener({
            AccountChooserActivity.startForResult(this,
                    AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND,
                    PaymentRequestType.SEND,
                    getString(R.string.to))
        })
    }

    override fun setCryptoCurrency(currency: String) {
        amountContainer.currencyBtc.setText(currency)
    }

    override fun disableCryptoTextChangeListener() {
        amountContainer.amountBtc.removeTextChangedListener(cryptoTextWatcher)
    }

    @SuppressLint("NewApi")
    override fun enableCryptoTextChangeListener() {
        amountContainer.amountBtc.addTextChangedListener(cryptoTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountBtc.setShowSoftInputOnFocus(false)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateCryptoTextField(amountString: String?) {
        amountContainer.amountBtc.setText(amountString)
    }

    override fun disableFiatTextChangeListener() {
        amountContainer.amountFiat.removeTextChangedListener(fiatTextWatcher)
    }

    @SuppressLint("NewApi")
    override fun enableFiatTextChangeListener() {
        amountContainer.amountFiat.addTextChangedListener(fiatTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountFiat.setShowSoftInputOnFocus(false)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateFiatTextField(amountString: String?) {
        amountContainer.amountFiat.setText(amountString)
    }

    // BTC Field
    @SuppressLint("NewApi")
    private fun setupBtcTextField() {
        amountContainer.amountBtc.setHint("0" + presenter.getDefaultDecimalSeparator() + "00")
        amountContainer.amountBtc.setSelectAllOnFocus(true)
        enableCryptoTextChangeListener()
    }

    // Fiat Field
    @SuppressLint("NewApi")
    private fun setupFiatTextField() {
        amountContainer.amountFiat.setHint("0" + presenter.getDefaultDecimalSeparator() + "00")
        amountContainer.amountFiat.setSelectAllOnFocus(true)
        enableFiatTextChangeListener()

    }

    private val cryptoTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }

        override fun afterTextChanged(editable: Editable) {
            presenter.updateFiatTextField(editable, amountContainer.amountBtc)
        }
    }

    private val fiatTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }

        override fun afterTextChanged(editable: Editable) {
            presenter.updateCryptoTextField(editable, amountContainer.amountFiat)
        }
    }

    private fun setupInitialAccount() {

        if (arguments != null) {
            presenter.selectSendingBtcAccount(arguments.getInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, -1))
        } else {
            presenter.selectDefaultSendingAccount()
        }
    }

    private fun setupSendingView() {
        fromContainer.fromAddressTextView.setOnClickListener({ startFromFragment() })
        fromContainer.fromArrowImage.setOnClickListener({ startFromFragment() })
    }

    override fun setSendingAddress(accountItem: ItemAccount) {
        fromContainer.fromAddressTextView.setText(accountItem.label)
    }

    override fun setReceivingHint(hint: Int) {
        toContainer.toAddressEditTextView.setHint(hint)
    }

    override fun resetAmounts() {
        val crypto = amountBtc.text
        amountBtc.setText(crypto)
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
        val adapter = FeePriorityAdapter(activity, presenter.getFeeOptionsForDropDown())

        spinnerPriority.setAdapter(adapter)

        spinnerPriority.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                when (position) {
                    0, 1 -> {
                        buttonSend.setEnabled(true)
                        textviewFeeAbsolute.setVisibility(View.VISIBLE)
                        textviewFeeTime.setVisibility(View.VISIBLE)
                        textInputLayout.setVisibility(View.GONE)
                        updateTotals()
                    }
//                    2 -> if (presenter.shouldShowAdvancedFeeWarning()) {
//                        alertCustomSpend()
//                    } else {
//                        displayCustomFeeField()
//                    }
                }

                val options = presenter.getFeeOptionsForDropDown().get(position)
                textviewFeeType.setText(options.getTitle())
                textviewFeeTime.setText(if (position != 2) options.getDescription() else null)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        })

        textviewFeeAbsolute.setOnClickListener({ spinnerPriority.performClick() })
        textviewFeeType.setText(R.string.fee_options_regular)
        textviewFeeTime.setText(R.string.fee_options_regular_time)

        //TODO this calls updateTotals multiple times on startup
        RxTextView.textChanges(amountContainer.amountBtc)
                .debounce(400, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateTotals() },{ it.printStackTrace() })

        RxTextView.textChanges(amountContainer.amountFiat)
                .debounce(400, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateTotals() },{ it.printStackTrace() })
    }

    internal fun updateTotals() {
        presenter.calculateTransactionAmounts(
                spendAll = false,
                amountToSendText = amountContainer.amountBtc.getText().toString(),
                feePriority = getFeePriority())
    }

    @FeeType.FeePriorityDef
    private fun getFeePriority(): Int {
        val position = spinnerPriority.getSelectedItemPosition()
        when (position) {
            1 -> return FeeType.FEE_OPTION_PRIORITY
            2 -> return FeeType.FEE_OPTION_CUSTOM
            else -> return FeeType.FEE_OPTION_REGULAR
        }
    }

    override fun getCustomFeeValue(): Long {
        val amount = edittextCustomFee.getText().toString()
        return if (!amount.isEmpty()) java.lang.Long.valueOf(amount) else 0
    }

    override fun showMaxAvailable() {
        max.visible()
        progressBarMaxAvailable.invisible()
    }

    override fun hideMaxAvailable() {
        max.invisible()
        progressBarMaxAvailable.visible()
    }

    override fun setUnconfirmedFunds(text: String) {
        unconfirmedFundsWarning.setText(text)

    }

    override fun updateFeeField(fee: String) {
        textviewFeeAbsolute.setText(fee)
    }

    override fun setMaxAvailable(amount: String) {
        max.setText(amount)
    }

    override fun setMaxAvailableColor(@ColorRes color: Int) {
        max.setTextColor(ContextCompat.getColor(context, color))
    }

    override fun setSpendAllAmount(textFromSatoshis: String) {
        amountContainer.amountBtc.setText(textFromSatoshis)
    }

    override fun showFeePriority() {
        textviewFeeType.visible()
        textviewFeeTime.visible()
        spaceTextView.visible()
    }

    override fun hideFeePriority() {
        textviewFeeType.gone()
        textviewFeeTime.gone()
        spaceTextView.gone()
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