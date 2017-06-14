package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.graphics.Color
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible

class TransactionSummaryDelegate<in T>(
        val activity: Activity,
        var btcExchangeRate: Double,
        var isBtc: Boolean,
        val listClickListener: BalanceListClickListener
) : AdapterDelegate<List<T>> {

    val stringUtils = StringUtils(activity)
    val prefsUtil = PrefsUtil(activity)
    val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    val dateUtil = DateUtil(activity)
    var contactsTransactionMap = mutableMapOf<String, String>()
    var notesTransactionMap = mutableMapOf<String, String>()

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is TransactionSummary

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            TxViewHolder(parent.inflate(R.layout.item_balance))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val txViewHolder = holder as TxViewHolder
        val tx = items[position] as TransactionSummary

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val btcBalance = tx.total.toLong() / 1e8
        val fiatBalance = btcExchangeRate * btcBalance

        txViewHolder.result.setTextColor(Color.WHITE)
        txViewHolder.timeSince.text = dateUtil.formatted(tx.time)

        when (tx.direction) {
            TransactionSummary.Direction.TRANSFERRED -> displayTransferred(txViewHolder)
            TransactionSummary.Direction.RECEIVED -> displayReceived(txViewHolder, tx)
            TransactionSummary.Direction.SENT -> displaySent(txViewHolder, tx)
            else -> throw IllegalStateException("Tx direction isn't SENT, RECEIVED or TRANSFERRED")
        }

        if (notesTransactionMap.containsKey(tx.hash)) {
            txViewHolder.note.text = notesTransactionMap[tx.hash]
            txViewHolder.note.visible()
        } else {
            txViewHolder.note.gone()
        }

        txViewHolder.result.text = getDisplaySpannable(tx.total.toDouble(), fiatBalance, fiatString)

        if (tx.direction == TransactionSummary.Direction.TRANSFERRED) {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_transferred_50, R.drawable.rounded_view_transferred))

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.context,
                    getColorForConfirmations(tx, R.color.product_gray_transferred_50, R.color.product_gray_transferred)))

        } else if (tx.direction == TransactionSummary.Direction.SENT) {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_red_50, R.drawable.rounded_view_red))

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.context,
                    getColorForConfirmations(tx, R.color.product_red_sent_50, R.color.product_red_sent)))

        } else {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_green_50, R.drawable.rounded_view_green))

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.context,
                    getColorForConfirmations(tx, R.color.product_green_received_50, R.color.product_green_received)))
        }

        txViewHolder.watchOnly.visibility = if (tx.isWatchOnly) View.VISIBLE else View.GONE
        txViewHolder.doubleSpend.visibility = if (tx.isDoubleSpend) View.VISIBLE else View.GONE

        txViewHolder.result.setOnClickListener {
            isBtc = !isBtc
            listClickListener.onValueClicked(isBtc)
        }

        txViewHolder.itemView.setOnClickListener {
            listClickListener.onTransactionClicked(
                    getRealTxPosition(txViewHolder.adapterPosition, items), position)
        }
    }

    fun onViewFormatUpdated(isBtc: Boolean) {
        this.isBtc = isBtc
    }

    fun onPriceUpdated(btcExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
    }

    fun onContactsMapUpdated(
            contactsTransactionMap: MutableMap<String, String>,
            notesTransactionMap: MutableMap<String, String>
    ) {
        this.contactsTransactionMap = contactsTransactionMap
        this.notesTransactionMap = notesTransactionMap
    }

    private fun displayTransferred(txViewHolder: TxViewHolder) {
        txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.MOVED)
    }

    private fun displayReceived(txViewHolder: TxViewHolder, tx: TransactionSummary) {
        if (contactsTransactionMap.containsKey(tx.hash)) {
            val contactName = contactsTransactionMap[tx.hash]
            txViewHolder.direction.text = stringUtils.getFormattedString(R.string.contacts_received, contactName)
        } else {
            txViewHolder.direction.text = stringUtils.getString(R.string.RECEIVED)
        }
    }

    private fun displaySent(txViewHolder: TxViewHolder, tx: TransactionSummary) {
        if (contactsTransactionMap.containsKey(tx.hash)) {
            val contactName = contactsTransactionMap[tx.hash]
            txViewHolder.direction.text = stringUtils.getFormattedString(R.string.contacts_sent, contactName)
        } else {
            txViewHolder.direction.text = stringUtils.getString(R.string.SENT)
        }
    }

    private fun getDisplaySpannable(btcAmount: Double, fiatAmount: Double, fiatString: String): Spannable {
        val spannable: Spannable
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getDisplayAmountWithFormatting(Math.abs(btcAmount)) + " " + getDisplayUnits())
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - getDisplayUnits().length,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatAmount)) + " " + fiatString)
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - 3,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    private fun getDisplayUnits(): String =
            monetaryUtil.btcUnits[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)].toString()

    private fun getColorForConfirmations(
            tx: TransactionSummary,
            @DrawableRes colorLight: Int,
            @DrawableRes colorDark: Int
    ): Int {
        return if (tx.confirmations < 3) colorLight else colorDark
    }

    private fun getRealTxPosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is TransactionSummary }
        return position - diff
    }

    private class TxViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.ts
        internal var direction: TextView = itemView.direction
        internal var watchOnly: TextView = itemView.watch_only
        internal var note: TextView = itemView.note
        internal var doubleSpend: ImageView = itemView.double_spend_warning
    }
}