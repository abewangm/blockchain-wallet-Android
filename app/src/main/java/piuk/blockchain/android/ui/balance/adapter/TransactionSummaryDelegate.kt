package piuk.blockchain.android.ui.balance.adapter

import android.graphics.Color
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.extensions.inflate

class TransactionSummaryDelegate<T> : AdapterDelegate<List<T>> {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return TxViewHolder(parent.inflate(R.layout.item_balance))
    }

    override fun onBindViewHolder(items: List<T>, position: Int, holder: RecyclerView.ViewHolder, payloads: List<*>) {
        val txViewHolder = holder as TxViewHolder
        val tx = items[position] as TransactionSummary

        val fiatString = "lol"
//                prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val btcBalance = tx.total.toLong() / 1e8
        val fiatBalance = "fiatBalance"
//                btcExchangeRate * btcBalance

        txViewHolder.result.setTextColor(Color.WHITE)
        txViewHolder.timeSince.text = tx.time.toString()

        when (tx.direction) {
            TransactionSummary.Direction.TRANSFERRED -> txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.MOVED)
            TransactionSummary.Direction.RECEIVED -> if (true) {
//                if (contactsTransactionMap.containsKey(tx.hash)) {
                val contactName = "contactName"
//                          contactsTransactionMap.get(tx.hash)
                txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.contacts_received, contactName)
            } else {
                txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.RECEIVED)
            }
            TransactionSummary.Direction.SENT -> if (true) {
//                if (contactsTransactionMap.containsKey(tx.hash)) {
                val contactName = "contactName"
//                        contactsTransactionMap.get(tx.hash)
                txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.contacts_sent, contactName)
            } else {
                txViewHolder.direction.text = txViewHolder.direction.context.getString(R.string.SENT)
            }
        }

//        if (notesTransactionMap.containsKey(tx.hash)) {
//            txViewHolder.note.setText(notesTransactionMap.get(tx.hash))
//            txViewHolder.note.visibility = View.VISIBLE
//        } else {
//            txViewHolder.note.visibility = View.GONE
//        }

//        txViewHolder.result.setText(getDisplaySpannable(tx.total.toLong().toDouble(), fiatBalance, fiatString))

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

//        txViewHolder.result.setOnClickListener { v ->
//            onViewFormatUpdated(!isBtc)
//            if (listClickListener != null) listClickListener.onValueClicked(isBtc)
//        }
//
//        txViewHolder.itemView.setOnClickListener { v ->
//            if (listClickListener != null) {
//                listClickListener.onTransactionClicked(
//                        getRealTxPosition(txViewHolder.getAdapterPosition()), position)
//            }
//        }
    }

    private fun getColorForConfirmations(
            tx: TransactionSummary,
            @DrawableRes colorLight: Int,
            @DrawableRes colorDark: Int
    ): Int {
        return if (tx.confirmations < 3) colorLight else colorDark
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is TransactionSummary

    private class TxViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {

        internal var result: TextView = view.findViewById(R.id.result) as TextView
        internal var timeSince: TextView = view.findViewById(R.id.ts) as TextView
        internal var direction: TextView = view.findViewById(R.id.direction) as TextView
        internal var watchOnly: TextView = view.findViewById(R.id.watch_only) as TextView
        internal var note: TextView = view.findViewById(R.id.note) as TextView
        internal var doubleSpend: ImageView = view.findViewById(R.id.double_spend_warning) as ImageView
    }
}