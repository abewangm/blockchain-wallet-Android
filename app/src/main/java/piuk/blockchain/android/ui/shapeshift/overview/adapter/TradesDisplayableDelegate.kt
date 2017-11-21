package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.app.Activity
import android.graphics.Color
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import info.blockchain.wallet.multiaddress.TransactionSummary
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
import java.math.BigDecimal
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
            items[position] is Displayable

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            TradeViewHolder(parent.inflate(R.layout.item_shapeshift_trade))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val viewHolder = holder as TradeViewHolder
        val tx = items[position] as Displayable

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val balance = when (tx.cryptoCurrency) {
            CryptoCurrencies.BTC -> BigDecimal(tx.total).divide(BigDecimal.valueOf(1e8))
            CryptoCurrencies.ETHER -> BigDecimal(tx.total).divide(BigDecimal.valueOf(1e18))
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        val fiatBalance = when (tx.cryptoCurrency) {
            CryptoCurrencies.BTC -> balance.multiply(BigDecimal(btcExchangeRate))
            CryptoCurrencies.ETHER -> balance.multiply(BigDecimal(ethExchangeRate))
            else -> throw IllegalArgumentException("BCC is not currently supported")
        }

        viewHolder.result.setTextColor(Color.WHITE)
        viewHolder.timeSince.text = dateUtil.formatted(tx.timeStamp)

        when (tx.direction) {
            TransactionSummary.Direction.TRANSFERRED -> displayTransferred(viewHolder, tx)
            TransactionSummary.Direction.RECEIVED -> displayReceived(viewHolder, tx)
            TransactionSummary.Direction.SENT -> displaySent(viewHolder, tx)
            else -> throw IllegalStateException("Tx direction isn't SENT, RECEIVED or TRANSFERRED")
        }

        viewHolder.result.text = getDisplaySpannable(
                tx.cryptoCurrency,
                tx.total.toDouble(),
                fiatBalance.toDouble(),
                fiatString
        )

        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        viewHolder.itemView.setOnClickListener {
            listClickListener.onTradeClicked(
                    getRealTxPosition(viewHolder.adapterPosition, items), position)
        }
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

    private fun displayTransferred(viewHolder: TradeViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.MOVED)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_transferred_50,
                R.drawable.rounded_view_transferred
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_gray_transferred_50,
                        R.color.product_gray_transferred
                ))
        )
    }

    private fun displayReceived(viewHolder: TradeViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.RECEIVED)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_green_50,
                R.drawable.rounded_view_green
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_green_received_50,
                        R.color.product_green_received
                ))
        )
    }

    private fun displaySent(viewHolder: TradeViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.SENT)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_red_50,
                R.drawable.rounded_view_red
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_red_sent_50,
                        R.color.product_red_sent
                ))
        )
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

    private fun getColorForConfirmations(
            tx: Displayable,
            @DrawableRes colorLight: Int,
            @DrawableRes colorDark: Int
    ) = if (tx.confirmations < getRequiredConfirmations(tx)) colorLight else colorDark

    private fun getRequiredConfirmations(tx: Displayable) =
            if (tx.cryptoCurrency == CryptoCurrencies.BTC) CONFIRMATIONS_BTC else CONFIRMATIONS_ETH

    private fun getRealTxPosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is Displayable }
        return position - diff
    }

    private class TradeViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
    }

    companion object {

        private const val CONFIRMATIONS_BTC = 3
        private const val CONFIRMATIONS_ETH = 12

    }

}