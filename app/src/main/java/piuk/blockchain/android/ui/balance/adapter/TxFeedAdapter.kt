package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.util.extensions.autoNotify
import kotlin.properties.Delegates

class TxFeedAdapter(
        activity: Activity,
        showCrypto: Boolean
//        noteMap: Map<String, String>,
//        listClickListener: TxFeedClickListener
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val summaryDelegate =
            DisplayableDelegate<Any>(activity, showCrypto
//                    , noteMap, listClickListener
            )

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(summaryDelegate)
        setHasStableIds(true)
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<Any> by Delegates.observable(emptyList()) { _, oldList, newList ->
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
    fun onViewFormatUpdated(showCrypto: Boolean) {
        summaryDelegate.onViewFormatUpdated(showCrypto)
        notifyDataSetChanged()
    }
}

interface TxFeedClickListener {

    fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int)

    fun onValueClicked(isBtc: Boolean)

}