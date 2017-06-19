package piuk.blockchain.android.ui.balance

import info.blockchain.wallet.payload.data.LegacyAddress
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.AccountActivity
import piuk.blockchain.android.ui.account.ConsolidatedAccount
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import javax.inject.Inject

class BalancePresenter : BasePresenter<BalanceView>() {

    @Inject lateinit var exchangeRateFactory: ExchangeRateFactory
    @Inject lateinit var transactionListDataManager: TransactionListDataManager
    @Inject lateinit var contactsDataManager: ContactsDataManager
    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var stringUtils: StringUtils
    @Inject lateinit var prefsUtil: PrefsUtil

    private var activeAccountAndAddressList: MutableList<ItemAccount> = mutableListOf()

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        val finalObject = Any() // TODO find default account position, pass into fetchTransactions

        val fetchTransactionsObservable = transactionListDataManager.fetchTransactions(finalObject, 50, 0)
        val updateTickerCompletable = exchangeRateFactory.updateTicker()
                .doOnComplete(view::onExchangeRateUpdated)
        val contactsObservable = contactsDataManager.fetchContacts()
                .andThen(contactsDataManager.contactsWithUnreadPaymentRequests)
        val fctxObservable = contactsDataManager.facilitatedTransactions


        /**
         * TODO: Ideally here we would concatenate:
         *
         * 0) Get all the user's accounts + legacy addresses
         * 1) Updating the ticker price
         * 2) Getting the transaction list
         * 3) Getting the Contacts list
         * 4) Get Facilitated Transactions
         *
         * All into one Observable and handling onNext in each component Observable.
         *
         */
    }

    fun getAllDisplayableAccounts() : List<Any> {
        activeAccountAndAddressList.clear()
        val accounts: List<ItemAccount> = payloadDataManager.accounts
                .filter { !it.isArchived }
                .map { it ->
                    val bal = payloadDataManager.getAddressBalance(it.xpub)
                    val balanceString = getBalanceString(true, bal.toLong())

                    return@map ItemAccount().apply {
                        label = it.label
                        displayBalance = balanceString
                        absoluteBalance = bal.toLong()
                    }
                }

        val legacyAddresses = payloadDataManager.legacyAddresses
                .filter { it.tag != LegacyAddress.ARCHIVED_ADDRESS }

        // Show "All Accounts" if necessary
        if (accounts.size > 1 || !legacyAddresses.isEmpty()) {
            val all = ConsolidatedAccount().apply {
                label = stringUtils.getString(R.string.all_accounts)
                type = ConsolidatedAccount.Type.ALL_ACCOUNTS
            }

            val bal = payloadDataManager.walletBalance
            val balance = getBalanceString(true, bal.toLong())

            activeAccountAndAddressList.add(ItemAccount().apply {
                label = all.label
                displayBalance = balance
                absoluteBalance = bal.toLong()
            })
        }

        // Show "Imported Addresses" if necessary
        if (!legacyAddresses.isEmpty()) {
            val importedAddresses = ConsolidatedAccount().apply {
                label = stringUtils.getString(R.string.imported_addresses)
                type = ConsolidatedAccount.Type.ALL_IMPORTED_ADDRESSES
            }

            val bal = payloadDataManager.importedAddressesBalance
            val balance = getBalanceString(true, bal.toLong())

            activeAccountAndAddressList.add(ItemAccount(
                    importedAddresses.label,
                    balance,
                    null,
                    bal.toLong(),
                    null))
        }

        return activeAccountAndAddressList
    }

    private fun getBalanceString(isBTC: Boolean, btcBalance: Long): String {
        val strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val lastPrice = ExchangeRateFactory.getInstance().getLastPrice(strFiat)
        val fiatBalance = lastPrice * (btcBalance / 1e8)

        return if (isBTC)
            monetaryUtil.getDisplayAmountWithFormatting(btcBalance) + " " + getDisplayUnits()
        else
            monetaryUtil.getFiatFormat(strFiat).format(fiatBalance) + " " + strFiat
    }

    fun getDisplayUnits(): String =
            monetaryUtil.btcUnits[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)].toString()

    val monetaryUtil: MonetaryUtil by lazy(LazyThreadSafetyMode.NONE) { MonetaryUtil(prefsUtil.getValue(
            PrefsUtil.KEY_BTC_UNITS,
            MonetaryUtil.UNIT_BTC
    )) }

    fun onRefreshRequested(): Unit = TODO("Update the list of transactions")

    fun onAccountChosen(account: AccountActivity): Unit = TODO("Filter the TX list by Account/LegacyAddress")

    fun onViewFormatChanged(): Unit = TODO("Change the stored view format (isBtc) and update the UI to reflect that")


}
