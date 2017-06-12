package piuk.blockchain.android.ui.balance.adapter

import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.ListDelegationAdapter


class BalanceAdapter(
        items: List<Any>
) : ListDelegationAdapter<List<Any>>(AdapterDelegatesManager(), items) {

    init {
        delegatesManager.addAdapterDelegate(HeaderDelegate())
        delegatesManager.addAdapterDelegate(TransactionSummaryDelegate())
    }

}