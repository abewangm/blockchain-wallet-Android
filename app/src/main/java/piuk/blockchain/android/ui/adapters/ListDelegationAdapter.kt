package piuk.blockchain.android.ui.adapters

open class ListDelegationAdapter<T : List<*>>(
        delegatesManager: AdapterDelegatesManager<T>,
        items: T
) : DelegationAdapter<T>(delegatesManager, items) {

    override fun getItemCount(): Int = items.size

}