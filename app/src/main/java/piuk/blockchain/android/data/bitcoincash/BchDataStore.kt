package piuk.blockchain.android.data.bitcoincash

import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataWallet
import piuk.blockchain.android.util.annotations.Mockable

/**
 * A simple data store class to cache the Bitcoin cash Wallet (bitcoin chain M/44H/0H)
 */
@Mockable
class BchDataStore {

    var bchWallet: BitcoinCashWallet? = null
    var bchMetadata: GenericMetadataWallet? = null

    fun clearBchData() {
        bchWallet = null
        bchMetadata = null
    }
}