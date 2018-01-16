package piuk.blockchain.android.data.bitcoincash

import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import info.blockchain.wallet.crypto.DeterministicAccount
import io.reactivex.Completable
import io.reactivex.Observable
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
        private val payloadDataManager: PayloadDataManager,
        private val bchDataStore: BchDataStore,
        private val metadataUtils: MetadataUtils,
        private val networkParameterUtils: NetworkParameterUtils,
        private val blockExplorer: BlockExplorer,
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
            fetchOrCreateBchMetadata(metadataNode, defaultLabel)
            restoreBchWallet(bchDataStore.bchMetadata!!)
            return@fromCallable Void.TYPE

        }.compose(RxUtil.applySchedulersToCompletable())
    }

    private fun fetchOrCreateBchMetadata(
            metadataKey: DeterministicKey,
            defaultLabel: String
    ) {

        val bchMetadataNode = metadataUtils.getMetadataNode(metadataKey, BitcoinCashWallet.METADATA_TYPE_EXTERNAL)
        val walletJson = bchMetadataNode.metadata

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
    private fun restoreBchWallet(walletMetadata: GenericMetadataWallet) {
        if (!payloadDataManager.isDoubleEncrypted) {
            bchDataStore.bchWallet = BitcoinCashWallet.restore(
                    blockExplorer,
                    networkParameterUtils.bitcoinCashParams,
                    BitcoinCashWallet.BITCOIN_COIN_PATH,
                    payloadDataManager.mnemonic,
                    "")

            var i = 0
            payloadDataManager.accounts.forEach {
                bchDataStore.bchWallet?.addAccount()
                walletMetadata.accounts[i].xpub = it.xpub
                i++
            }
        } else {

            bchDataStore.bchWallet = BitcoinCashWallet.createWatchOnly(
                    blockExplorer,
                    networkParameterUtils.bitcoinCashParams)

            var i = 0
            payloadDataManager.accounts.forEach {
                bchDataStore.bchWallet?.addWatchOnlyAccount(it.xpub)
                walletMetadata.accounts[i].xpub = it.xpub
                i++
            }
        }
    }

    fun getActiveXpubs(): List<String> {

        val result = mutableListOf<String>()

        bchDataStore.bchMetadata?.accounts?.forEachIndexed { i, account ->
            if (!account.isArchived) {
                result.add(bchDataStore.bchWallet?.getAccountPubB58(i)!!)
            }
        }

        return result
    }

    fun updateAllBalances() = bchDataStore.bchWallet?.updateAllBalances(getActiveXpubs())

    fun getAddressBalance(address: String) = bchDataStore.bchWallet?.getAddressBalance(address)

    fun getWalletBalance() = bchDataStore.bchWallet?.getWalletBalance()

    fun getAddressTransactions(address: String, limit: Int, offset: Int) =
            bchDataStore.bchWallet?.getTransactions(getActiveXpubs(), address, limit, offset)

    fun getWalletTransactions(limit: Int, offset: Int) =
            bchDataStore.bchWallet?.getTransactions(getActiveXpubs(), null, limit, offset)

    /**
     * Returns all non-archived accounts
     * @return Generic account data that contains label and xpub/address
     */
    fun getActiveAccounts(): List<GenericMetadataAccount> {

        val active = mutableListOf<GenericMetadataAccount>()

        bchDataStore.bchMetadata?.accounts?.forEach {
            if (!it.isArchived) {
                active.add(it)
            }
        }

        return active
    }

    fun getDefaultAccountPosition(): Int = bchDataStore.bchMetadata?.defaultAcccountIdx ?: 0

    fun getDefaultDeterministicAccount(): DeterministicAccount? =
            bchDataStore.bchWallet?.accounts?.get(getDefaultAccountPosition())

    fun getDefaultGenericMetadataAccount(): GenericMetadataAccount? =
            bchDataStore.bchMetadata?.accounts?.get(getDefaultAccountPosition())

    fun getReceiveAddressAtPosition(accountIndex: Int, addressIndex: Int): String? =
            bchDataStore.bchWallet?.getReceiveAddressAtPositionBch(accountIndex, addressIndex)

    fun getNextReceiveCashAddress(accountIndex: Int) =
            Observable.fromCallable {
                bchDataStore.bchWallet?.getNextReceiveCashAddress(accountIndex)
            }

    fun getNextChangeCashAddress(accountIndex: Int) =
            Observable.fromCallable {
                bchDataStore.bchWallet?.getNextChangeCashAddress(accountIndex)
            }

    fun getNextChangeCashAddress(accountIndex: Int, addressIndex: Int) =
            Observable.fromCallable {
                bchDataStore.bchWallet?.getChangeCashAddressAt(accountIndex, addressIndex)
            }

    fun incrementNextReceiveAddressBch(xpub: String) =
            Completable.fromCallable {
                bchDataStore.bchWallet?.incrementNextReceiveAddressBch(xpub)
            }

    fun incrementNextChangeAddressBch(xpub: String) =
            Completable.fromCallable {
                bchDataStore.bchWallet?.incrementNextChangeAddressBch(xpub)
            }

    fun isOwnAddress(address: String) =
            bchDataStore.bchWallet?.isOwnAddress(address)

    /**
     * Converts any Bitcoin Cash address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Account or legacy address label
     */
    fun getLabelFromBchAddress(address: String): String? {
        val xpub = bchDataStore.bchWallet?.getXpubFromAddress(address)

        return bchDataStore.bchMetadata?.accounts?.find { it.xpub == xpub }?.label
    }
}
