package piuk.blockchain.android.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

abstract class DelegationAdapter<T>constructor(
        protected var delegatesManager: AdapterDelegatesManager<T> = AdapterDelegatesManager<T>(),
        var items: T
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            delegatesManager.onBindViewHolder(items, position, holder, null)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<*>?) {
        delegatesManager.onBindViewHolder(items, position, holder, payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return delegatesManager.getItemViewType(items, position)
    }

}