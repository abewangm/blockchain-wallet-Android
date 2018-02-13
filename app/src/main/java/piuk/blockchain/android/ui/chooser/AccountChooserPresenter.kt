package piuk.blockchain.android.ui.chooser

import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.contacts.ContactsDataManager
import piuk.blockchain.android.data.contacts.ContactsPredicates
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import javax.inject.Inject

class AccountChooserPresenter @Inject internal constructor(
        private val exchangeRateFactory: ExchangeRateFactory,
        private val walletAccountHelper: WalletAccountHelper,
        private val payloadDataManager: PayloadDataManager,
        private val bchDataManager: BchDataManager,
        private val prefsUtil: PrefsUtil,
        private val currencyState: CurrencyState,
        private val stringUtils: StringUtils,
        private val contactsDataManager: ContactsDataManager
) : BasePresenter<AccountChooserView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private val itemAccounts = ArrayList<ItemAccount>()

    override fun onViewReady() {
        val mode = view.accountMode

        when (mode) {
            AccountMode.ShapeShift -> loadShapeShiftAccounts()
            AccountMode.ContactsOnly -> loadContactsOnly()
            AccountMode.Bitcoin -> loadBitcoinOnly()
            AccountMode.BitcoinSummary -> loadBitcoinSummary()
            AccountMode.BitcoinCash -> loadBitcoinCashOnly()
            AccountMode.BitcoinCashSend -> loadBitcoinCashSend()
            AccountMode.BitcoinCashSummary -> loadBitcoinCashSummary()
        }
    }

    private fun loadBitcoinOnly() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
        parseBtcAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .flatMap { parseBtcImportedList() }
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadBitcoinCashOnly() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
        parseBchAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadBitcoinCashSend() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))
        parseBchAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .flatMap { parseBchImportedList() }
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadBitcoinSummary() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }
        val accounts = payloadDataManager.accounts
                .filter { !it.isArchived }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || legacyAddresses.isNotEmpty()) {
            val bigIntBalance = payloadDataManager.walletBalance

            itemAccounts.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                displayBalance = getBtcBalanceString(
                        currencyState.isDisplayingCryptoCurrency,
                        bigIntBalance.toLong(),
                        exchangeRateFactory::getLastBtcPrice,
                        ::getBtcDisplayUnits
                )
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        parseBtcAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .doOnSuccess {
                    // Show "Imported Addresses" if wallet contains legacy addresses
                    if (!legacyAddresses.isEmpty()) {
                        val bigIntBalance = payloadDataManager.importedAddressesBalance

                        itemAccounts.add(ItemAccount().apply {
                            displayBalance = getBtcBalanceString(
                                    currencyState.isDisplayingCryptoCurrency,
                                    bigIntBalance.toLong(),
                                    exchangeRateFactory::getLastBtcPrice,
                                    ::getBtcDisplayUnits
                            )
                            label = stringUtils.getString(R.string.imported_addresses)
                            absoluteBalance = bigIntBalance.toLong()
                            type = ItemAccount.TYPE.ALL_LEGACY
                        })
                    }
                }
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadBitcoinCashSummary() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.wallets)))

        val accounts = bchDataManager.getActiveAccounts()

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || bchDataManager.getImportedAddressBalance() > BigInteger.ZERO) {
            val bigIntBalance = bchDataManager.getWalletBalance()

            itemAccounts.add(ItemAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                displayBalance = getBtcBalanceString(
                        currencyState.isDisplayingCryptoCurrency,
                        bigIntBalance.toLong(),
                        exchangeRateFactory::getLastBchPrice,
                        ::getBchDisplayUnits
                )
                absoluteBalance = bigIntBalance.toLong()
                type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
            })
        }

        parseBchAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .doOnSuccess {
                    // Show "Imported Addresses" if wallet contains legacy addresses
                    if (bchDataManager.getImportedAddressBalance() > BigInteger.ZERO) {
                        val bigIntBalance = bchDataManager.getImportedAddressBalance()

                        itemAccounts.add(ItemAccount().apply {
                            displayBalance = getBtcBalanceString(
                                    currencyState.isDisplayingCryptoCurrency,
                                    bigIntBalance.toLong(),
                                    exchangeRateFactory::getLastBchPrice,
                                    ::getBchDisplayUnits
                            )
                            label = stringUtils.getString(R.string.imported_addresses)
                            absoluteBalance = bigIntBalance.toLong()
                            type = ItemAccount.TYPE.ALL_LEGACY
                        })
                    }
                }
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadShapeShiftAccounts() {
        itemAccounts.add(ItemAccount(stringUtils.getString(R.string.bitcoin)))
        parseBtcAccountList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .flatMap { parseEthAccount() }
                .doOnSuccess { itemAccounts.add(ItemAccount(stringUtils.getString(R.string.bitcoin_cash))) }
                .flatMap { parseBchAccountList() }
                .subscribe(
                        { view.updateUi(itemAccounts) },
                        { Timber.e(it) }
                )
    }

    private fun loadContactsOnly() {
        parseContactsList()
                .compose(RxUtil.addSingleToCompositeDisposable(this))
                .subscribe(
                        {
                            when {
                                !it.isEmpty() -> view.updateUi(itemAccounts)
                                else -> view.showNoContacts()
                            }
                        },
                        { Timber.e(it) }
                )
    }

    private fun parseContactsList(): Single<List<Contact>> {
        return if (!view.isContactsEnabled) {
            Single.just(ArrayList())
        } else {
            contactsDataManager.getContactList()
                    .filter(ContactsPredicates.filterByConfirmed())
                    .toList()
                    .doOnSuccess { contacts ->
                        if (!contacts.isEmpty()) {
                            itemAccounts.add(
                                    ItemAccount(stringUtils.getString(R.string.contacts_title))
                            )
                            contacts.mapTo(itemAccounts) {
                                ItemAccount(
                                        null,
                                        null,
                                        null,
                                        null,
                                        it,
                                        ""
                                )
                            }
                        }
                    }
        }
    }

    private fun parseBtcAccountList(): Single<List<ItemAccount>> =
            getBtcAccountList().doOnNext { itemAccounts.addAll(it) }.singleOrError()

    private fun parseBchAccountList(): Single<List<ItemAccount>> =
            getBchAccountList().doOnNext { itemAccounts.addAll(it) }.singleOrError()

    private fun parseBtcImportedList(): Single<List<ItemAccount>> {
        return getBtcImportedList().doOnNext {
            if (!it.isEmpty()) {
                itemAccounts.add(ItemAccount(stringUtils.getString(R.string.imported_addresses)))
                itemAccounts.addAll(it)
            }
        }.singleOrError()
    }

    private fun parseBchImportedList(): Single<List<ItemAccount>> {
        return getBchImportedList().doOnNext {
            if (!it.isEmpty()) {
                itemAccounts.add(ItemAccount(stringUtils.getString(R.string.imported_addresses)))
                itemAccounts.addAll(it)
            }
        }.singleOrError()
    }

    private fun parseEthAccount(): Single<List<ItemAccount>> {
        return getEthAccount().doOnNext {
            itemAccounts.add(ItemAccount(stringUtils.getString(R.string.ether)))
            itemAccounts.add(it)
        }.map { itemAccounts.toList() }
                .singleOrError()
    }

    private fun getBtcAccountList(): Observable<List<ItemAccount>> =
            Observable.just(walletAccountHelper.getHdAccounts())

    private fun getBtcImportedList(): Observable<List<ItemAccount>> =
            Observable.just(ArrayList(walletAccountHelper.getLegacyAddresses()))

    private fun getBchAccountList(): Observable<List<ItemAccount>> =
            Observable.just(ArrayList(walletAccountHelper.getHdBchAccounts()))

    private fun getBchImportedList(): Observable<List<ItemAccount>> =
            Observable.just(ArrayList(walletAccountHelper.getLegacyBchAddresses()))

    private fun getEthAccount(): Observable<ItemAccount> =
            Observable.just(walletAccountHelper.getEthAccount()[0])

    private fun getBtcBalanceString(
            isBtc: Boolean,
            btcBalance: Long,
            lastPrice: (String) -> Double,
            displayUnits: () -> String
    ): String {
        val strFiat = getFiatCurrency()
        val fiatBalance = lastPrice(strFiat) * (btcBalance / 1e8)
        var balance = monetaryUtil.getDisplayAmountWithFormatting(btcBalance)
        // Replace 0.0 with 0 to match web
        if (balance == "0.0") balance = "0"

        return if (isBtc) {
            "$balance ${displayUnits()}"
        } else {
            "${monetaryUtil.getFiatFormat(strFiat).format(fiatBalance)} $strFiat"
        }
    }

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBchDisplayUnits() = monetaryUtil.getBchUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

}
