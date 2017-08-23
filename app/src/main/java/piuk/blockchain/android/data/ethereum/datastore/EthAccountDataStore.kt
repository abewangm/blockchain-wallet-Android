package piuk.blockchain.android.data.ethereum.datastore

import info.blockchain.wallet.ethereum.data.EthAccount
import io.reactivex.Observable
import piuk.blockchain.android.data.stores.Optional

interface EthAccountDataStore {

    fun getEthAccount(): Observable<Optional<EthAccount>>

}