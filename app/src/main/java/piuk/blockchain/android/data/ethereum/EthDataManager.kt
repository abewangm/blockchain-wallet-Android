package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAccount
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.util.MetadataUtil
import io.reactivex.Observable
import piuk.blockchain.android.data.ethereum.datastore.EthDataStore
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import timber.log.Timber

class EthDataManager(
        private val ethService: EthService,
        private val ethDataStore: EthDataStore,
        private val payloadManager: PayloadManager,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    /**
     * Stores the ETH address associated with the user's wallet for the duration of the session.
     *
     * @param address The ETH address belonging to the logged in user
     */
    fun initEthAccount(address: String) = ethService.storeEthAccount(address)

    /**
     * Clears the currently stored ETH address from memory.
     */
    fun clearEthAccount() = ethService.clearEthAccount()

    /**
     * Returns an [EthAccount] object for a given ETH address as an [Observable]. An [EthAccount]
     * contains a list of transactions associated with the account, as well as a final balance.
     * Calling this function also stores the [EthAccount] in the [EthDataStore] for caching.
     *
     * @return An [Observable] wrapping an [EthAccount]
     */
    fun fetchEthAccount(): Observable<EthAccount> = rxPinning.call<EthAccount> {
        ethDataStore.fetchEthAccount()
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns a list of [EthDisplayable] objects associated with a user's ETH address specifically
     * for displaying in the transaction list. These are cached and may be empty if the account
     * hasn't previously been fetched.
     *
     * @return An [Observable] list of [EthDisplayable] objects
     */
    fun getEthTransactions(): Observable<List<EthDisplayable>> = rxPinning.call<List<EthDisplayable>> {
        ethDataStore.getEthAccount()
                .flatMapIterable { ethAccount -> ethAccount.transactions
                            .map { EthDisplayable(ethAccount, it) }
                }
                .map { listOf(it) }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns true if a given ETH address is associated with an Ethereum contract, which is
     * currently unsupported. This should be used to validate any proposed destination address for
     * funds.
     *
     * @param address The ETH address to be queried
     * @return An [Observable] returning true or false based on the address's contract status
     */
    fun getIfContract(address: String): Observable<Boolean> = rxPinning.call<Boolean> {
        ethService.getIfContract(address)
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns EthereumWallet stored in metadata. If metadata entry doens't exists it will be inserted.
     *
     * @param defaultLabel The ETH address default label to be used if metadata entry doesn't exist
     * @return An [Observable] returning EthereumWallet
     */
    fun getEthereumWallet(defaultLabel: String) : Observable<EthereumWallet> = rxPinning.call<EthereumWallet> {
        Observable.fromCallable{fetchOrCreateEthereumWallet(defaultLabel)}
    }

    private fun fetchOrCreateEthereumWallet(defaultLabel: String) : EthereumWallet {

        val masterKey = payloadManager.payload.hdWallets[0].masterKey
        val metadataNode = MetadataUtil.deriveMetadataNode(masterKey)

        var ethWallet = EthereumWallet.load(metadataNode)

        if (ethWallet == null) {
            ethWallet =  EthereumWallet(masterKey, defaultLabel)
            ethWallet.save()
        }

        return ethWallet
    }
}