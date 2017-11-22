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
import piuk.blockchain.android.data.transactions.Displayable
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.extensions.getContext
import piuk.blockchain.android.util.extensions.inflate
import timber.log.Timber
import java.text.DecimalFormat

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


        if(trade.timestamp > 0) {
            viewHolder.timeSince.text = dateUtil.formatted(trade.timestamp / 1000)
        } else {
            //Existing Web issue - no available date to set
            viewHolder.timeSince.text = ""
        }
        viewHolder.direction.text = trade.status.toString()

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

        //trade.quote.pair could have worked but isn't being saved in kv-store
        //htf do we know the crypto currency

//        viewHolder.result.text = getDisplaySpannable(
//                getCryptoAmount(trade.quote.pair),
//                0.01,
//                100.0,
//                fiatString
//        )

        displayTradeColour(viewHolder, trade)

        viewHolder.result.setOnClickListener { listClickListener.onValueClicked(!showCrypto) }
        viewHolder.layout.setOnClickListener {
            listClickListener.onTradeClicked(
                    getRealTradePosition(viewHolder.adapterPosition, items), position
            )
        }
    }

    fun getCryptoAmount(pair: String): CryptoCurrencies {
        return CryptoCurrencies.valueOf(pair.split("_").get(1))
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

    private fun displayTradeColour(viewHolder: TradeViewHolder, trade: Trade) {

        when (trade.status) {
            Trade.STATUS.COMPLETE -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_complete)
                viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_green_medium))
            }
            Trade.STATUS.FAILED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))
            }
            Trade.STATUS.NO_DEPOSITS -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))
            }
            Trade.STATUS.RECEIVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_inprogress)
                viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_gray_transferred))
            }
            Trade.STATUS.RESOLVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_green)
                viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_green_medium))
            }
        }
    }

    private fun getDisplaySpannable(
            cryptoCurrency: CryptoCurrencies,
            cryptoAmount: Double,
            fiatAmount: Double,
            fiatString: String
    ): Spannable {
        val spannable: Spannable
        if (showCrypto) {
            if (cryptoCurrency == CryptoCurrencies.BTC) {
                spannable = Spannable.Factory.getInstance().newSpannable(
                        "${monetaryUtil.getDisplayAmountWithFormatting(Math.abs(cryptoAmount))} ${getDisplayUnits()}")
                spannable.setSpan(
                        RelativeSizeSpan(0.67f),
                        spannable.length - getDisplayUnits().length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                        .run { format(cryptoAmount / 1e18) }

                spannable = Spannable.Factory.getInstance().newSpannable(
                        "$number ETH")
                spannable.setSpan(
                        RelativeSizeSpan(0.67f),
                        spannable.length - "ETH".length,
                        spannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    "${monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatAmount))} $fiatString")
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - 3,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    private fun getDisplayUnits(): String =
            monetaryUtil.getBtcUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)]

    private fun getRealTradePosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is Displayable }
        return position - diff
    }

    private class TradeViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
        internal var layout: RelativeLayout = itemView.trade_row
    }
}