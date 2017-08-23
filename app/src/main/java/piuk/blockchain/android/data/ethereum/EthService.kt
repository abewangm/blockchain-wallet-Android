package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.data.EthAccount
import io.reactivex.Observable
import piuk.blockchain.android.util.annotations.WebRequest

class EthService(private val ethAccountApi: EthAccountApi) {

    private var address: String? = null

    /**
     * Stores the ETH address associated with the user's wallet for the duration of the session.
     *
     * @param address The ETH address belonging to the logged in user
     */
    internal fun storeEthAccount(address: String) {
        this.address = address
    }

    /**
     * Clears the currently stored ETH address from memory.
     */
    internal fun clearEthAccount() {
        address = null
    }

    /**
     * Returns an [EthAccount] object for a given ETH address as an [Observable]. An [EthAccount]
     * contains a list of transactions associated with the account, as well as a final balance.
     *
     * @return An [Observable] wrapping an [EthAccount]
     */
    @WebRequest
    fun getEthAccount(): Observable<EthAccount> = ethAccountApi.getEthAccount(address)

    /**
     * Returns true if a given ETH address is associated with an Ethereum contract, which is
     * currently unsupported. This should be used to validate any proposed destination address for
     * funds.
     *
     * @param address The ETH address to be queried
     * @return An [Observable] returning true or false based on the address's contract status
     */
    @WebRequest
    internal fun getIfContract(address: String): Observable<Boolean> = ethAccountApi.getIfContract(address)

}