package piuk.blockchain.android.ui.exchange.newexchange

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_new_exchange.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume
import javax.inject.Inject

class NewExchangeActivity : BaseMvpActivity<NewExchangeView, NewExchangePresenter>(), NewExchangeView {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var newExchangePresenter: NewExchangePresenter

    private val btcSymbol = CryptoCurrencies.BTC.symbol.toUpperCase()
    private val ethSymbol = CryptoCurrencies.ETHER.symbol.toUpperCase()

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

        onViewReady()
    }

    override fun showFrom(cryptoCurrency: CryptoCurrencies) = when (cryptoCurrency) {
        CryptoCurrencies.BTC -> showFromBtc()
        CryptoCurrencies.ETHER -> showFromEth()
        CryptoCurrencies.BCC -> throw IllegalArgumentException("BCC not supported")
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = newExchangePresenter

    override fun getView() = this

    private fun showFromBtc() {
        textview_unit_from.text = btcSymbol
        textview_unit_to.text = ethSymbol
        imageview_to_dropdown.gone()
        // TODO: Check if dropdown is necessary by comparing account quantity
        imageview_from_dropdown.visible()
    }

    private fun showFromEth() {
        textview_unit_from.text = ethSymbol
        textview_unit_to.text = btcSymbol
        imageview_to_dropdown.visible()
        // TODO: Check if dropdown is necessary by comparing account quantity
        imageview_from_dropdown.gone()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, NewExchangeActivity::class.java))
        }

    }

}