package piuk.blockchain.android.ui.adapters

import android.support.v4.util.SparseArrayCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup


/**
 * @param T The type of the data source
 */
class AdapterDelegatesManager<T> {

    private val PAYLOADS_EMPTY_LIST = ArrayList<Any>()

    private var delegates: SparseArrayCompat<AdapterDelegate<T>> = SparseArrayCompat()

    fun addAdapterDelegate(delegate: AdapterDelegate<T>): AdapterDelegatesManager<T> {
        var viewType = delegates.size()
        while (delegates[viewType] != null) {
            viewType++
        }

        delegates.put(viewType, delegate)
        return this
    }

    fun removeAdapterDelegate(delegate: AdapterDelegate<T>): AdapterDelegatesManager<T> {
        val index = delegates.indexOfValue(delegate)
        delegates.removeAt(index)
        return this
    }

    fun getItemViewType(items: T, position: Int): Int {
        for (i in 0 until delegates.size()) {
            val delegate = delegates.get(i)
            if (delegate.isForViewType(items, position)) {
                return delegates.keyAt(i)
            }
        }

        throw NullPointerException("Delegate not found for item at position $position.")
    }

    fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        if (parent == null) throw NullPointerException("ViewGroup parent was null")

        val delegate = getDelegateForViewType(viewType)
                ?: throw NullPointerException("No delegate found for view type $viewType")

        return delegate.onCreateViewHolder(parent)
    }

    fun onBindViewHolder(items: T, position: Int, viewHolder: RecyclerView.ViewHolder?, payloads: List<*>?) {
        if (viewHolder == null) throw NullPointerException("ViewHolder was null")

        val delegate = getDelegateForViewType(viewHolder.itemViewType)
                ?: throw NullPointerException("No delegate found for item at position $position for view type  ${viewHolder.itemViewType}")

        delegate.onBindViewHolder(items, position, viewHolder, payloads ?: PAYLOADS_EMPTY_LIST)
    }

    fun onBindViewHolder(items: T, position: Int, viewHolder: RecyclerView.ViewHolder?) =
            onBindViewHolder(items, position, viewHolder, PAYLOADS_EMPTY_LIST)

    /**
     * Returns the [AdapterDelegate] for a given view type
     */
    fun getDelegateForViewType(viewType: Int): AdapterDelegate<T>? = delegates.get(viewType)

    /**
     * Returns the view type for a given [AdapterDelegate]
     */
    fun getViewType(delegate: AdapterDelegate<T>): Int {
        val index = delegates.indexOfValue(delegate)
        if (index == -1) {
            return -1
        }
        return delegates.keyAt(index)
    }

}