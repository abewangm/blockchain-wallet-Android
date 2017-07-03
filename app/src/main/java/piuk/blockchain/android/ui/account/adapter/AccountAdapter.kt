package piuk.blockchain.android.ui.account.adapter

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.ListDelegationAdapter
import piuk.blockchain.android.util.extensions.autoNotify
import kotlin.properties.Delegates

class AccountAdapter(
        accountHeadersListener: AccountHeadersListener
) : ListDelegationAdapter<List<Any>>(AdapterDelegatesManager(), emptyList()) {

    val createWalletDelegate = CreateWalletDelegate<Any>(accountHeadersListener)
    val importAddressDelegate = ImportAddressDelegate<Any>(accountHeadersListener)
    val accountDelegate = AccountDelegate<Any>(accountHeadersListener)

    init {
        delegatesManager.apply {
            addAdapterDelegate(createWalletDelegate)
            addAdapterDelegate(importAddressDelegate)
            addAdapterDelegate(accountDelegate)
        }
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

}

interface AccountHeadersListener {

    fun onCreateNewClicked()

    fun onImportAddressClicked()

    fun onAccountClicked(correctedPosition: Int)
}