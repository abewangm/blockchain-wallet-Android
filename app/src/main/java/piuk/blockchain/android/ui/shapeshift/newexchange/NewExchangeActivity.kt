package piuk.blockchain.android.ui.shapeshift.newexchange

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import kotlinx.android.synthetic.main.activity_new_exchange.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume
import javax.inject.Inject

class NewExchangeActivity : BaseMvpActivity<NewExchangeView, NewExchangePresenter>(), NewExchangeView {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var newExchangePresenter: NewExchangePresenter

    private val btcSymbol = CryptoCurrencies.BTC.symbol.toUpperCase()
    private val ethSymbol = CryptoCurrencies.ETHER.symbol.toUpperCase()
    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_exchange)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        button_continue.setOnClickListener { presenter.onContinuePressed() }
        textview_use_max.setOnClickListener { presenter.onMaxPressed() }
        textview_use_min.setOnClickListener { presenter.onMinPressed() }
        imageview_from_dropdown.setOnClickListener { presenter.onFromChooserClicked() }
        imageview_to_dropdown.setOnClickListener { presenter.onToChooserClicked() }
        imageview_switch_currency.setOnClickListener { presenter.onSwitchCurrencyClicked() }

        onViewReady()
    }

    override fun updateUi(
            fromCurrency: CryptoCurrencies,
            displayDropDown: Boolean,
            fromLabel: String,
            toLabel: String
    ) {
        // Titles
        textview_to_address.text = toLabel
        textview_from_address.text = fromLabel

        when (fromCurrency) {
            CryptoCurrencies.BTC -> showFromBtc(displayDropDown)
            CryptoCurrencies.ETHER -> showFromEth(displayDropDown)
            CryptoCurrencies.BCC -> throw IllegalArgumentException("BCC not supported")
        }
    }

    override fun launchAccountChooserActivityTo() {
        // TODO: Test me
        AccountChooserActivity.startForResult(
                this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND,
                PaymentRequestType.REQUEST,
                getString(R.string.to)
        )
    }

    override fun launchAccountChooserActivityFrom() {
        // TODO: Test me
        AccountChooserActivity.startForResult(
                this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND,
                PaymentRequestType.SEND,
                getString(R.string.from)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO:
    }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(message)
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun finishPage() = finish()

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = newExchangePresenter

    override fun getView() = this

    private fun showFromBtc(displayDropDown: Boolean) {
        // Units
        textview_unit_from.text = btcSymbol
        textview_unit_to.text = ethSymbol
        // Visibility
        imageview_to_dropdown.gone()
        if (displayDropDown) imageview_from_dropdown.visible()
    }

    private fun showFromEth(displayDropDown: Boolean) {
        // Units
        textview_unit_from.text = ethSymbol
        textview_unit_to.text = btcSymbol
        // Visibility
        if (displayDropDown) imageview_to_dropdown.visible()
        imageview_from_dropdown.gone()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, NewExchangeActivity::class.java))
        }

    }

}