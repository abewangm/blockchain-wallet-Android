package piuk.blockchain.android.ui.receive

import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.annotations.Mockable
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import java.math.RoundingMode
import java.util.*

@Mockable
class WalletAccountHelper(
        private val payloadManager: PayloadManager,
        private val stringUtils: StringUtils,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val currencyState: CurrencyState,
        private val ethDataManager: EthDataManager
) {
    private val btcUnitType: Int by unsafeLazy { prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC) }
    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(btcUnitType) }
    private val btcUnit: String by unsafeLazy { monetaryUtil.getBtcUnit(btcUnitType) }
    private val fiatUnit: String by unsafeLazy { prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY) }
    private val btcExchangeRate: Double by unsafeLazy { exchangeRateFactory.getLastBtcPrice(fiatUnit) }

    /**
     * Returns a list of [ItemAccount] objects containing both HD accounts and [LegacyAddress]
     * objects, eg from importing accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getAccountItems(): List<ItemAccount> = when (currencyState.cryptoCurrency) {
        CryptoCurrencies.BTC -> mutableListOf<ItemAccount>().apply {
            // V3
            addAll(getHdAccounts())
            // V2l
            addAll(getLegacyAddresses())
        }.toList()

        else -> getEthAccount().toList()
    }

    /**
     * Returns a list of [ItemAccount] objects containing only HD accounts.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getHdAccounts(): List<ItemAccount> {
        val list = payloadManager.payload?.hdWallets?.get(0)?.accounts ?: Collections.emptyList<Account>()
        // Skip archived account
        return list.filterNot { it.isArchived }
                .map {
                    ItemAccount(
                            it.label,
                            getAccountBalance(it, btcExchangeRate, fiatUnit, btcUnit),
                            null,
                            getAccountAbsoluteBalance(it),
                            it,
                            it.xpub
                    )
                }
    }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getLegacyAddresses() = payloadManager.payload.legacyAddressList
            // Skip archived address
            .filterNot { it.tag == LegacyAddress.ARCHIVED_ADDRESS }
            .map {
                // If address has no label, we'll display address
                var labelOrAddress: String? = it.label
                if (labelOrAddress == null || labelOrAddress.trim { it <= ' ' }.isEmpty()) {
                    labelOrAddress = it.address
                }

                // Watch-only tag - we'll ask for xpriv scan when spending from
                var tag: String? = null
                if (it.isWatchOnly) {
                    tag = stringUtils.getString(R.string.watch_only)
                }

                ItemAccount(
                        labelOrAddress,
                        getAddressBalance(it, btcExchangeRate, fiatUnit, btcUnit),
                        tag,
                        getAddressAbsoluteBalance(it),
                        it,
                        it.address
                )
            }

    /**
     * Returns a list of [ItemAccount] objects containing only [LegacyAddress] objects,
     * specifically from the list of address book entries.
     *
     * @return Returns a list of [ItemAccount] objects
     */
    fun getAddressBookEntries() = payloadManager.payload.addressBook?.map {
        ItemAccount(
                if (it.label.isNullOrEmpty()) it.address else it.label,
                "",
                stringUtils.getString(R.string.address_book_label),
                null,
                null,
                it.address
        )
    } ?: emptyList()

    fun getDefaultAccount(): ItemAccount = when (currencyState.cryptoCurrency) {
        CryptoCurrencies.BTC -> getDefaultBtcAccount()
        else -> getDefaultEthAccount()
    }

    fun getEthAccount() = mutableListOf<ItemAccount>().apply {
        add(getDefaultEthAccount())
    }

    /**
     * Returns the balance of an [Account] in Satoshis
     */
    private fun getAccountAbsoluteBalance(account: Account) =
            payloadManager.getAddressBalance(account.xpub).toLong()

    /**
     * Returns the balance of an [Account], formatted for display
     */
    private fun getAccountBalance(
            account: Account,
            btcExchange: Double,
            fiatUnit: String,
            btcUnit: String
    ): String {

        val btcBalance = getAccountAbsoluteBalance(account)

        return if (!currencyState.isDisplayingCryptoCurrency) {
            val fiatBalance = btcExchange * (btcBalance / 1e8)
            "(${monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance)} $fiatUnit)"
        } else {
            "(${monetaryUtil.getDisplayAmount(btcBalance)} $btcUnit)"
        }
    }

    /**
     * Returns the balance of a [LegacyAddress] in Satoshis
     */
    private fun getAddressAbsoluteBalance(legacyAddress: LegacyAddress) =
            payloadManager.getAddressBalance(legacyAddress.address).toLong()

    /**
     * Returns the balance of a [LegacyAddress], formatted for display
     */
    private fun getAddressBalance(
            legacyAddress: LegacyAddress,
            btcExchange: Double,
            fiatUnit: String,
            btcUnit: String
    ): String {

        val btcBalance = getAddressAbsoluteBalance(legacyAddress)

        return if (!currencyState.isDisplayingCryptoCurrency) {
            val fiatBalance = btcExchange * (btcBalance / 1e8)
            "(${monetaryUtil.getFiatFormat(fiatUnit).format(fiatBalance)} $fiatUnit)"
        } else {
            "(${monetaryUtil.getDisplayAmount(btcBalance)} $btcUnit)"
        }
    }

    private fun getDefaultBtcAccount(): ItemAccount {
        val account = payloadManager.payload.hdWallets[0].accounts[payloadManager.payload.hdWallets[0].defaultAccountIdx]
        return ItemAccount(
                account.label,
                getAccountBalance(account, btcExchangeRate, fiatUnit, btcUnit),
                null,
                getAccountAbsoluteBalance(account),
                account,
                account.xpub
        )
    }

    private fun getDefaultEthAccount(): ItemAccount {
        val ethModel = ethDataManager.getEthResponseModel()
        val ethAccount = ethDataManager.getEthWallet()!!.account
        var amount = Convert.fromWei(ethModel?.getTotalBalance().toString(), Convert.Unit.ETHER)
        amount = amount.setScale(8, RoundingMode.HALF_DOWN)
        return ItemAccount(
                ethAccount?.label,
                "($amount ETH)",
                null,
                0,
                ethAccount,
                ethAccount?.address
        )
    }

}
