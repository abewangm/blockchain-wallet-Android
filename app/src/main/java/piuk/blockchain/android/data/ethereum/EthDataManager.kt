package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.data.EthAccount
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.util.MetadataUtil
import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import timber.log.Timber

class EthDataManager(
        private val ethAccountApi: EthAccountApi,
        private val ethService: EthService,
        private val ethDataStore: EthDataStore,
        private val payloadManager: PayloadManager,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)
    private var address: String? = null
    private var ethAccount: EthAddressResponse? = null

    /**
     * Stores the ETH address associated with the user's wallet for the duration of the session.
     *
     * @param address The ETH address belonging to the logged in user
     */
    internal fun storeEthAccountAddress(address: String) {
        this.address = address
    }

    /**
     * Clears the currently stored ETH address and [EthAddressResponse] from memory.
     */
    internal fun clearEthAccountDetails() {
        address = null
        ethAccount = null
    }

    /**
     * Returns an [EthAddressResponse] object for a given ETH address as an [Observable]. An
     * [EthAddressResponse] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [EthAddressResponse].
     *
     * @return An [Observable] wrapping an [EthAddressResponse]
     */
    fun fetchEthAddress(): Observable<EthAddressResponse> = rxPinning.call<EthAddressResponse> {
        ethAccountApi.getEthAddress(address)
                .doOnNext { account -> ethAccount = account }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [EthAddressResponse] object
     */
    fun getEthAddress() = ethAccount

    /**
     * Returns a steam of [EthTransaction] objects associated with a user's ETH address specifically
     * for displaying in the transaction list. These are cached and may be empty if the account
     * hasn't previously been fetched.
     *
     * @return An [Observable] stream of [EthTransaction] objects
     */
    fun getEthTransactions(): Observable<EthTransaction> {
        ethAccount?.let {
            return Observable.just(it)
                    .flatMapIterable { it.transactions }
                    .compose(RxUtil.applySchedulersToObservable())
        }

        return Observable.empty()
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
        ethAccountApi.getIfContract(address)
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns EthereumWallet stored in metadata. If metadata entry doens't exists it will be inserted.
     *
     * @param defaultLabel The ETH address default label to be used if metadata entry doesn't exist
     * @return An [Observable] returning EthereumWallet
     */
    fun getEthereumWallet(defaultLabel: String): Observable<EthereumWallet> = rxPinning.call<EthereumWallet> {
        Observable.fromCallable { fetchOrCreateEthereumWallet(defaultLabel) }
    }

    private fun fetchOrCreateEthereumWallet(defaultLabel: String): EthereumWallet {
        val masterKey = payloadManager.payload.hdWallets[0].masterKey
        val metadataNode = MetadataUtil.deriveMetadataNode(masterKey)

        var ethWallet = EthereumWallet.load(metadataNode)

        if (ethWallet == null) {
            ethWallet = EthereumWallet(masterKey, defaultLabel)
            ethWallet.save()
        }

        return ethWallet
    }
}