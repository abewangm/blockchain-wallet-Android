package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.ListDelegationAdapter


class BalanceAdapter(
        activity: Activity,
        items: List<Any>,
        btcExchangeRate: Double,
        isBtc: Boolean,
        listClickListener: BalanceListClickListener
) : ListDelegationAdapter<List<Any>>(AdapterDelegatesManager(), items) {

    val summaryDelegate = TransactionSummaryDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)
    val fctxDelegate = FctxDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)

    init {
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(summaryDelegate)
        delegatesManager.addAdapterDelegate(fctxDelegate)
    }

    fun onViewFormatUpdated(isBtc: Boolean) {
        summaryDelegate.onViewFormatUpdated(isBtc)
        fctxDelegate.onViewFormatUpdated(isBtc)
        notifyDataSetChanged()
    }

    fun onDataSetUpdated(items: List<Any>) {
        this.items = items
        notifyDataSetChanged()
        // TODO Diff Util
    }

}

interface BalanceListClickListener {

    fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int)

    fun onValueClicked(isBtc: Boolean)

    fun onFctxClicked(fctxId: String)

    fun onFctxLongClicked(fctxId: String)

}