package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.getContext
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume

class FctxDelegate<in T>(
        val activity: Activity,
        var btcExchangeRate: Double,
        var isBtc: Boolean,
        val listClickListener: BalanceListClickListener
) : AdapterDelegate<T> {

    val dateUtil = DateUtil(activity)
    val stringUtils = StringUtils(activity)
    val prefsUtil = PrefsUtil(activity)
    val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is ContactTransactionModel

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            FctxViewHolder(parent.inflate(R.layout.item_balance))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val viewHolder = holder as FctxViewHolder
        val model = items[position] as ContactTransactionModel
        val transaction = model.facilitatedTransaction
        val contactName = model.contactName

        // Click listener
        holder.itemView.setOnClickListener { listClickListener.onFctxClicked(transaction.id) }
        // Long click listener
        holder.itemView.setOnLongClickListener {
            consume { listClickListener.onFctxLongClicked(transaction.id) }
        }

        val btcBalance = transaction.intendedAmount / 1e8
        val fiatBalance = btcExchangeRate * btcBalance

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val amountSpannable = getDisplaySpannable(transaction.intendedAmount.toDouble(), fiatBalance, fiatString)

        viewHolder.result.text = amountSpannable
        viewHolder.timeSince.text = dateUtil.formatted(transaction.lastUpdated)
        viewHolder.contactName.text = contactName

        if (transaction.state == FacilitatedTransaction.STATE_DECLINED) {
            // TODO: Received/Sent?
            viewHolder.note.setText(R.string.contacts_receiving_declined)
            viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))

        } else if (transaction.state == FacilitatedTransaction.STATE_CANCELLED) {
            // TODO: Received/Sent?
            viewHolder.note.setText(R.string.contacts_receiving_cancelled)
            viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))

        } else if (transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS) {
            if (transaction.role == FacilitatedTransaction.ROLE_PR_RECEIVER) {

                viewHolder.note.setText(R.string.contacts_transaction_accept_or_decline)
                viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.primary_blue_accent))
                displayReceiving(viewHolder)

            } else if (transaction.role == FacilitatedTransaction.ROLE_RPR_INITIATOR) {

                viewHolder.note.setText(R.string.contacts_transaction_awaiting_response)
                viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.product_gray_hint))
                displaySending(viewHolder)
            }
        } else if (transaction.state == FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT) {
            if (transaction.role == FacilitatedTransaction.ROLE_RPR_RECEIVER) {

                viewHolder.note.setText(R.string.contacts_transaction_ready_to_send)
                viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.primary_blue_accent))
                displaySending(viewHolder)

            } else if (transaction.role == FacilitatedTransaction.ROLE_PR_INITIATOR) {

                viewHolder.note.setText(R.string.contacts_transaction_payment_requested)
                viewHolder.note.setTextColor(getResolvedColor(viewHolder, R.color.product_gray_hint))
                displayReceiving(viewHolder)
            }
        }

    }

    fun onViewFormatUpdated(isBtc: Boolean, btcFormat: Int) {
        this.isBtc = isBtc
        monetaryUtil.updateUnit(btcFormat)
    }

    fun onPriceUpdated(btcExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
    }

    private fun displayReceiving(viewHolder: FctxViewHolder) {
        viewHolder.direction.setText(R.string.receiving)
        viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_green_received_50))
        viewHolder.result.setBackgroundResource(R.drawable.rounded_view_green_50)
    }

    private fun displayReceived(viewHolder: FctxViewHolder) {
        viewHolder.direction.setText(R.string.RECEIVED)
        viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_green_received))
        viewHolder.result.setBackgroundResource(R.drawable.rounded_view_green)
    }

    private fun displaySending(viewHolder: FctxViewHolder) {
        viewHolder.direction.setText(R.string.sending)
        viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_red_sent_50))
        viewHolder.result.setBackgroundResource(R.drawable.rounded_view_red_50)
    }

    private fun displaySent(viewHolder: FctxViewHolder) {
        viewHolder.direction.setText(R.string.SENT)
        viewHolder.direction.setTextColor(getResolvedColor(viewHolder, R.color.product_red_sent))
        viewHolder.result.setBackgroundResource(R.drawable.rounded_view_red)
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int {
        return ContextCompat.getColor(viewHolder.getContext(), color)
    }

    private fun getDisplaySpannable(btcAmount: Double, fiatAmount: Double, fiatString: String): Spannable {
        val spannable: Spannable
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    "${monetaryUtil.getDisplayAmountWithFormatting(Math.abs(btcAmount))} ${getDisplayUnits()}")
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - getDisplayUnits().length,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
            monetaryUtil.btcUnits[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)].toString()

    private class FctxViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
        internal var watchOnly: TextView = itemView.watch_only
        internal var note: TextView = itemView.note
        internal var doubleSpend: ImageView = itemView.double_spend_warning
        internal var contactName: TextView = itemView.contact_name

        init {
            doubleSpend.gone()
            watchOnly.gone()
            note.visible()
        }

    }

}