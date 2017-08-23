package piuk.blockchain.android.data.ethereum.datastore

import info.blockchain.wallet.ethereum.data.EthAccount
import io.reactivex.Observable
import piuk.blockchain.android.data.stores.Optional
import piuk.blockchain.android.data.stores.PersistentStore
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class EthMemoryStore : EthAccountDataStore, PersistentStore<EthAccount> {

    private var ethAccount: Optional<EthAccount> = Optional.None

    override fun store(data: EthAccount): Observable<EthAccount> {
        ethAccount = Optional.Some(data)
        return Observable.just((ethAccount as Optional.Some<EthAccount>).element)
    }

    override fun getEthAccount(): Observable<Optional<EthAccount>> =
            Observable.just(ethAccount)

    override fun invalidate() {
        ethAccount = Optional.None
    }

}