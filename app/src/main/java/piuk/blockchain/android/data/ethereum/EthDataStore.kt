package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import piuk.blockchain.android.util.annotations.Mockable

/**
 * A simple data store class to cache the Ethereum Wallet
 */
@Mockable
class EthDataStore {

    var ethWallet: EthereumWallet? = null
    var ethAddressResponse: EthAddressResponse? = null

    fun clearEthData() {
        ethWallet = null
        ethAddressResponse = null
    }
}