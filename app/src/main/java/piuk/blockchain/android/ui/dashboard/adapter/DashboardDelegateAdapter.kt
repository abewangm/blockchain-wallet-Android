package piuk.blockchain.android.ui.dashboard.adapter

import android.app.Activity
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.balance.adapter.AnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.ChartsState
import piuk.blockchain.android.util.extensions.autoNotify
import kotlin.properties.Delegates

class DashboardDelegateAdapter(
        activity: Activity
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val chartDelegate = ChartDelegate<Any>(activity)

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(AnnouncementDelegate())
        delegatesManager.addAdapterDelegate(chartDelegate)
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
     * Updates the state of the Charts card without causing a refresh of the entire View.
     */
    fun updateChartState(chartsState: ChartsState) {
        chartDelegate.updateChartState(chartsState)
    }

}