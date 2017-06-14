package piuk.blockchain.android.ui.adapters


/**
 * This adapter implementation is ready to use, and can be extended and overridden as needed. This
 * is designed for items organised in a [List] rather than some more complex data structure.
 * Simply add [AdapterDelegate] objects to the [AdapterDelegatesManager] to handle the different
 * View types.
 */
open class ListDelegationAdapter<T : List<*>>(
        delegatesManager: AdapterDelegatesManager<T>,
        items: T
) : DelegationAdapter<T>(delegatesManager, items) {

    override fun getItemCount(): Int = items.size

}