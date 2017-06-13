package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.ListDelegationAdapter
import piuk.blockchain.android.util.extensions.autoNotify
import kotlin.properties.Delegates


class BalanceAdapter(
        activity: Activity,
        btcExchangeRate: Double,
        isBtc: Boolean,
        listClickListener: BalanceListClickListener
) : ListDelegationAdapter<List<Any>>(AdapterDelegatesManager(), emptyList()) {

    val summaryDelegate = TransactionSummaryDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)
    val fctxDelegate = FctxDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)

    /**
     *  Observe items list and automatically notify the adapter of changes to the data based on the
     *  comparison we make here, which is a simple equality check.
     */
    override var items: List<Any> by Delegates.observable(emptyList()) {
        _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

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

    fun onContactsMapChanged(
            contactsMap: MutableMap<String, String>,
            notesTransactionMap: MutableMap<String, String>
    ) = summaryDelegate.onContactsMapChanged(contactsMap, notesTransactionMap)

}

interface BalanceListClickListener {

    fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int)

    fun onValueClicked(isBtc: Boolean)

    fun onFctxClicked(fctxId: String)

    fun onFctxLongClicked(fctxId: String)

}