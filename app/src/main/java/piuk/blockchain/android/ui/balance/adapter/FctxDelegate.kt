package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import kotlinx.android.synthetic.main.item_contact_transactions.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.ContactTransactionModel
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.SpanFormatter
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.helperfunctions.consume

class FctxDelegate<in T>(
        val activity: Activity,
        var btcExchangeRate: Double,
        var isBtc: Boolean,
        val listClickListener: BalanceListClickListener
) : AdapterDelegate<T> {

    val stringUtils = StringUtils(activity)
    val prefsUtil = PrefsUtil(activity)
    val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is ContactTransactionModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            FctxViewHolder(parent.inflate(R.layout.item_contact_transactions))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val fctxViewHolder = holder as FctxViewHolder
        val model = items[position] as ContactTransactionModel
        val transaction = model.facilitatedTransaction
        val contactName = model.contactName

        // Click listener
        holder.itemView.setOnClickListener { listClickListener.onFctxClicked(transaction.id) }
        // Long click listener
        holder.itemView.setOnLongClickListener {
            consume { listClickListener.onFctxLongClicked(transaction.id) }
        }

        fctxViewHolder.indicator.visibility = View.GONE
        fctxViewHolder.title.setTextColor(ContextCompat.getColor(fctxViewHolder.title.context, R.color.black))

        val btcBalance = transaction.intendedAmount / 1e8
        val fiatBalance = btcExchangeRate * btcBalance

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val amountSpannable = getDisplaySpannable(transaction.intendedAmount.toDouble(), fiatBalance, fiatString)

        if (transaction.state == FacilitatedTransaction.STATE_DECLINED) {
            fctxViewHolder.title.setText(R.string.contacts_receiving_declined)

        } else if (transaction.state == FacilitatedTransaction.STATE_CANCELLED) {
            fctxViewHolder.title.setText(R.string.contacts_receiving_cancelled)

        } else if (transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS) {
            if (transaction.role == FacilitatedTransaction.ROLE_PR_RECEIVER) {

                val display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_receiving_from_contact_waiting_to_accept),
                        amountSpannable,
                        contactName)
                fctxViewHolder.title.text = display
                fctxViewHolder.indicator.visibility = View.VISIBLE

            } else if (transaction.role == FacilitatedTransaction.ROLE_RPR_INITIATOR) {

                val display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_sending_to_contact_waiting),
                        amountSpannable,
                        contactName)
                fctxViewHolder.title.text = display
            }
        } else if (transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT) {
            if (transaction.role == FacilitatedTransaction.ROLE_RPR_RECEIVER) {

                val display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_payment_requested_ready_to_send),
                        amountSpannable,
                        contactName)
                fctxViewHolder.title.text = display
                fctxViewHolder.indicator.visibility = View.VISIBLE

            } else if (transaction.role == FacilitatedTransaction.ROLE_PR_INITIATOR) {

                val display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_requesting_from_contact_waiting_for_payment),
                        amountSpannable,
                        contactName)
                fctxViewHolder.title.text = display
            }
        }

        fctxViewHolder.subtitle.text = transaction.note
    }

    fun onViewFormatUpdated(isBtc: Boolean) {
        this.isBtc = isBtc
    }

    fun onPriceUpdated(btcExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
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

    private class FctxViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var indicator: ImageView = itemView.imageview_indicator
        internal var title: TextView = itemView.transaction_title
        internal var subtitle: TextView = itemView.transaction_subtitle

    }

}