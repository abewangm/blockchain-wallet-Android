package piuk.blockchain.android.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

/**
 * An abstract class which delegates all important functions to registered [AdapterDelegate]
 * objects. Extend this class if necessary, or extend [ListDelegationAdapter] for a more complete
 * implementation that already handles [List] objects correctly (which will be what you want 99%
 * of the time).
 */
abstract class DelegationAdapter<T> constructor(
        protected var delegatesManager: AdapterDelegatesManager<T> = AdapterDelegatesManager<T>(),
        open var items: T
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            delegatesManager.onBindViewHolder(items, position, holder, null)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<*>?) =
            delegatesManager.onBindViewHolder(items, position, holder, payloads)

    override fun getItemViewType(position: Int): Int =
            delegatesManager.getItemViewType(items, position)

}