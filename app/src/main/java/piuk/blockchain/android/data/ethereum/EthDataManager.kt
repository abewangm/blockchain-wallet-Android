package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.data.EthAccount
import info.blockchain.wallet.ethereum.data.EthTransaction
import io.reactivex.Observable
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil

class EthDataManager(
        private val ethAccountApi: EthAccountApi,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)
    private var address: String? = null
    private var ethAccount: EthAccount? = null

    /**
     * Stores the ETH address associated with the user's wallet for the duration of the session.
     *
     * @param address The ETH address belonging to the logged in user
     */
    internal fun storeEthAccount(address: String) {
        this.address = address
    }

    /**
     * Clears the currently stored ETH address and [EthAccount] from memory.
     */
    internal fun clearEthAccount() {
        address = null
        ethAccount = null
    }

    /**
     * Returns an [EthAccount] object for a given ETH address as an [Observable]. An [EthAccount]
     * contains a list of transactions associated with the account, as well as a final balance.
     * Calling this function also caches the [EthAccount].
     *
     * @return An [Observable] wrapping an [EthAccount]
     */
    fun fetchEthAccount(): Observable<EthAccount> = rxPinning.call<EthAccount> {
        ethAccountApi.getEthAccount(address)
                .doOnNext { account -> ethAccount = account }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [EthAccount] object
     */
    fun getEthAccount() = ethAccount

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

}