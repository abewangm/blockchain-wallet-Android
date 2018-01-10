package piuk.blockchain.android.data.bitcoincash

import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import io.reactivex.Completable
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.MetadataUtils
import piuk.blockchain.android.util.NetworkParameterUtils
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class BchDataManager(
        val payloadDataManager: PayloadDataManager,
        val bchDataStore: BchDataStore,
        val networkParameterUtils: NetworkParameterUtils,
        val metadataUtils: MetadataUtils,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    /**
     * Clears the currently stored BCH wallet from memory.
     */
    fun clearBchAccountDetails() = bchDataStore.clearBchData()

    /**
     * Fetches EthereumWallet stored in metadata. If metadata entry doesn't exists it will be created.
     *
     * @param defaultLabel The ETH address default label to be used if metadata entry doesn't exist
     * @return An [Completable]
     */
    fun initBchWallet(metadataNode: DeterministicKey, defaultLabel: String) = rxPinning.call {
        Completable.fromCallable {
            fetchOrCreateBchWallet(metadataNode, defaultLabel)
            return@fromCallable Void.TYPE

        }.compose(RxUtil.applySchedulersToCompletable())
    }

    private fun fetchOrCreateBchWallet(
            metadataKey: DeterministicKey,
            defaultLabel: String
    ) {

        restoreBchWallet()

        val bchMetadataNode = metadataUtils.getMetadataNode(metadataKey, BitcoinCashWallet.METADATA_TYPE_EXTERNAL)
        var walletJson = bchMetadataNode.getMetadata()

        if (walletJson != null) {
            //Fetch wallet
            bchDataStore.bchMetadata = GenericMetadataWallet.fromJson(walletJson)

        } else {
            // Create
            val accountTotal = payloadDataManager.accounts.size
            val bchAccounts = arrayListOf<GenericMetadataAccount>()

            for (i in 1..accountTotal) {
                val name: String
                when (i) {
                    in 2..accountTotal -> name = defaultLabel + " " + i
                    else -> name = defaultLabel
                }
                bchAccounts.add(GenericMetadataAccount(name, false))
            }
            bchDataStore.bchMetadata = GenericMetadataWallet()

            bchDataStore.bchMetadata?.run {
                accounts = bchAccounts
                isHasSeen = true
            }
        }

    }

    /**
     * Restore bitcoin cash wallet
     */
    private fun restoreBchWallet() {
        if (!payloadDataManager.isDoubleEncrypted) {
            bchDataStore.bchWallet = BitcoinCashWallet.restore(
                    networkParameterUtils.bitcoinCashParams,
                    BitcoinCashWallet.BITCOIN_COIN_PATH,
                    payloadDataManager.mnemonic,
                    "")

            for(i in payloadDataManager.accounts) {
                bchDataStore.bchWallet?.addAccount()
            }
        } else {

            bchDataStore.bchWallet = BitcoinCashWallet.createWatchOnly(networkParameterUtils.bitcoinCashParams)

            for(i in payloadDataManager.accounts) {
                bchDataStore.bchWallet?.addWatchOnlyAccount(i.xpub)
            }
        }
    }
}
