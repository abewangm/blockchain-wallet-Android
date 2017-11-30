package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.app.Activity
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import info.blockchain.wallet.shapeshift.data.Trade
import kotlinx.android.synthetic.main.item_shapeshift_trade.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.extensions.getContext
import piuk.blockchain.android.util.extensions.inflate
import java.math.BigDecimal

class TradesDisplayableDelegate<in T>(
        activity: Activity,
        private var btcExchangeRate: Double,
        private var ethExchangeRate: Double,
        private var showCrypto: Boolean,
        private val listClickListener: TradesListClickListener
) : AdapterDelegate<T> {

    private val prefsUtil = PrefsUtil(activity)
    private val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    private val dateUtil = DateUtil(activity)

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is Trade

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            TradeViewHolder(parent.inflate(R.layout.item_shapeshift_trade))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val viewHolder = holder as TradeViewHolder
        val trade = items[position] as Trade

        if (trade.timestamp > 0) {
            viewHolder.timeSince.text = dateUtil.formatted(trade.timestamp / 1000)
        } else {
            //Existing Web issue - no available date to set
            viewHolder.timeSince.text = ""
        }

        viewHolder.result.text = getDisplaySpannable(
                trade.acquiredCoinType,
                trade.quote.withdrawalAmount ?: BigDecimal.ZERO
        )

        viewHolder.status.setText(determineStatus(viewHolder, trade))

        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        viewHolder.layout.setOnClickListener { listClickListener.onTradeClicked(trade.quote.deposit) }
    }

    fun onViewFormatUpdated(isBtc: Boolean, btcFormat: Int) {
        this.showCrypto = isBtc
        monetaryUtil.updateUnit(btcFormat)
    }

    fun onPriceUpdated(btcExchangeRate: Double, ethExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
        this.ethExchangeRate = ethExchangeRate
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int {
        return ContextCompat.getColor(viewHolder.getContext(), color)
    }

    private fun determineStatus(viewHolder: TradeViewHolder, trade: Trade) =

            when (trade.status) {
                Trade.STATUS.COMPLETE -> {
                    viewHolder.result.setBackgroundResource(R.drawable.rounded_view_complete)
                    viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_green_medium))
                    R.string.shapeshift_complete_title
                }
                Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> {
                    viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                    viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))
                    R.string.shapeshift_failed_title
                }
                Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> {
                    viewHolder.result.setBackgroundResource(R.drawable.rounded_view_inprogress)
                    viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_gray_transferred))
                    R.string.shapeshift_in_progress_title
                }
            }

    private fun getDisplaySpannable(
            cryptoCurrency: String,
            cryptoAmount: BigDecimal
    ): Spannable {
        val spannable: Spannable

        val displayAmount: String
        val unit: String

        if (showCrypto) {

            val cryptoAmount = when (cryptoCurrency.toUpperCase()) {
                CryptoCurrencies.ETHER.symbol -> monetaryUtil.getEthFormat().format(cryptoAmount)
                CryptoCurrencies.BTC.symbol -> monetaryUtil.getBtcFormat().format(cryptoAmount)
                else -> monetaryUtil.getBtcFormat().format(cryptoAmount)//Coin type not specified
            }
            unit = cryptoCurrency
            displayAmount = "$cryptoAmount $cryptoCurrency"
        } else {

            val fiatAmount = when (cryptoCurrency.toUpperCase()) {
                CryptoCurrencies.ETHER.symbol -> cryptoAmount.multiply(BigDecimal.valueOf(ethExchangeRate))
                CryptoCurrencies.BTC.symbol -> cryptoAmount.multiply(BigDecimal.valueOf(btcExchangeRate))
                else -> BigDecimal.ZERO//Coin type not specified
            }

            unit = getPreferedFiatUnit();
            displayAmount = "${monetaryUtil.getFiatFormat(unit).format(fiatAmount.abs())} $unit"
        }

        spannable = Spannable.Factory.getInstance().newSpannable(displayAmount)
        spannable.setSpan(
                RelativeSizeSpan(0.67f),
                spannable.length - unit.length,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }

    private fun getPreferedFiatUnit() = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private class TradeViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var status: TextView = itemView.status
        internal var layout: RelativeLayout = itemView.trade_row
    }
}