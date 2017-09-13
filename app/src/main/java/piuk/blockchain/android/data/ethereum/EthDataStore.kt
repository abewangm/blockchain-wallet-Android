package piuk.blockchain.android.data.ethereum

import info.blockchain.wallet.ethereum.EthereumWallet
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.util.annotations.Mockable

/**
 * A simple data store class to cache the Ethereum Wallet
 */
@Mockable
class EthDataStore {

    var ethWallet: EthereumWallet? = null
    var ethAddressResponse: CombinedEthModel? = null

    fun clearEthData() {
        ethWallet = null
        ethAddressResponse = null
    }
}