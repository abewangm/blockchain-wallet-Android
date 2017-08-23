package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.extensions.autoNotify
import kotlin.properties.Delegates


class BalanceAdapter(
        activity: Activity,
        btcExchangeRate: Double,
        isBtc: Boolean,
        listClickListener: BalanceListClickListener
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val summaryDelegate =
            TransactionSummaryDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)
    private val fctxDelegate =
            FctxDelegate<Any>(activity, btcExchangeRate, isBtc, listClickListener)

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(summaryDelegate)
        delegatesManager.addAdapterDelegate(fctxDelegate)
        delegatesManager.addAdapterDelegate(AnnouncementDelegate<Any>())
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<Any> by Delegates.observable(emptyList()) {
        _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    /**
     * Notifies the adapter that the View format (ie, whether or not to show BTC) has been changed.
     * Will rebuild the entire adapter.
     */
    fun onViewFormatUpdated(isBtc: Boolean, btcFormat: Int) {
        summaryDelegate.onViewFormatUpdated(isBtc, btcFormat)
        fctxDelegate.onViewFormatUpdated(isBtc, btcFormat)
        notifyDataSetChanged()
    }

    /**
     * Notifies the adapter that the Contacts/Transaction map and the Notes/Transaction map have
     * been updated. Will rebuild the entire adapter.
     */
    fun onContactsMapChanged(
            transactionDisplayMap: MutableMap<String, ContactTransactionDisplayModel>
    ) {
        summaryDelegate.onContactsMapUpdated(transactionDisplayMap)
        notifyDataSetChanged()
    }

    /**
     * Notifies the adapter that the BTC exchange rate for the selected currency has been updated.
     * Will rebuild the entire adapter.
     */
    fun onPriceUpdated(lastPrice: Double) {
        summaryDelegate.onPriceUpdated(lastPrice)
        fctxDelegate.onPriceUpdated(lastPrice)
        notifyDataSetChanged()
    }

}

interface BalanceListClickListener {

    fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int)

    fun onValueClicked(isBtc: Boolean)

    fun onFctxClicked(fctxId: String)

    fun onFctxLongClicked(fctxId: String)

}