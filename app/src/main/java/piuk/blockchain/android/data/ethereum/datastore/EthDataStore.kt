package piuk.blockchain.android.data.ethereum.datastore

import info.blockchain.wallet.ethereum.data.EthAccount
import io.reactivex.Observable
import piuk.blockchain.android.data.stores.DefaultFetchStrategy
import piuk.blockchain.android.data.stores.FreshFetchStrategy
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class EthDataStore(
        private val memoryStore: EthMemoryStore,
        private val webSource: Observable<EthAccount>
) {

    fun getEthAccount() =
            DefaultFetchStrategy(webSource, memoryStore.getEthAccount(), memoryStore).fetch()

    fun fetchEthAccount() =
            FreshFetchStrategy(webSource, memoryStore).fetch()

}