package piuk.blockchain.android.ui.balance

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.include_no_transaction_message.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.balance.adapter.ItemAccount2
import piuk.blockchain.android.ui.balance.adapter.TxFeedAdapter
import piuk.blockchain.android.ui.balance.adapter.TxFeedClickListener
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.onItemSelectedListener
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView, TxFeedClickListener {

    // Adapters
    private var accountsAdapter: AccountsAdapter? = null
    private var txFeedAdapter: TxFeedAdapter? = null

    @Inject lateinit var balancePresenter: BalancePresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_balance)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_container.setProgressViewEndTarget(
                false,
                ViewUtils.convertDpToPixel(72F + 20F, context).toInt()
        )
        swipe_container.setOnRefreshListener { presenter.onRefreshRequested() }
        swipe_container.setColorSchemeResources(
                R.color.product_green_medium,
                R.color.primary_blue_medium,
                R.color.product_red_medium
        )

        textview_balance.setOnClickListener { presenter.invertViewType() }
        currency_header.setSelectionListener { presenter.updateSelectedCurrency(it) }

        onViewReady()
    }

    override fun setupAccountsAdapter(accounts: List<ItemAccount2>) {

        if (accountsAdapter == null) {
            accountsAdapter = AccountsAdapter(
                    context,
                    R.layout.spinner_balance_header,
                    accounts
            ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }

            accounts_spinner.adapter = accountsAdapter
        }

        handleAccountSpinnerVisibility()

        accounts_spinner.setOnTouchListener({ _, event ->
            event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
        })

        accounts_spinner.onItemSelectedListener = onItemSelectedListener {
            presenter.onAccountChosen(it)
            recyclerview.scrollToPosition(0)
        }

        presenter.onAccountChosen(0)
    }

    override fun selectDefaultAccount() {
        if (accountsAdapter?.isNotEmpty ?: false) accounts_spinner.setSelection(0, false)
    }

    override fun updateAccountsDataSet(accountsList: List<ItemAccount2>) {
        accountsAdapter?.updateAccountList(accountsList)
        handleAccountSpinnerVisibility()
    }

    fun handleAccountSpinnerVisibility() {
        if (accountsAdapter?.showSpinner() ?: false) {
            layout_spinner.visible()
        } else {
            layout_spinner.gone()
        }
    }

    override fun createPresenter() = balancePresenter

    override fun getMvpView() = this

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.FAILURE, UiState.EMPTY -> onEmptyState()
            UiState.CONTENT -> onContentLoaded()
            UiState.LOADING -> setShowRefreshing(true)
        }
    }

    private fun onEmptyState() {
        setShowRefreshing(false)
        no_transaction_include.visible()

        when (currency_header.selectedCurrency) {
            CryptoCurrencies.BTC -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin)
//                button_get_bitcoin.setOnClickListener { presenter.getBitcoinClicked() }
                description.setText(R.string.transaction_occur_when_bitcoin)
            }
            CryptoCurrencies.ETHER -> {
                button_get_bitcoin.setText(R.string.onboarding_get_eth)
                button_get_bitcoin.setOnClickListener { startReceiveFragmentEth() }
                description.setText(R.string.transaction_occur_when_eth)
            }
            CryptoCurrencies.BCH -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin_cash)
                button_get_bitcoin.setOnClickListener { startReceiveFragmentBch() }
                description.setText(R.string.transaction_occur_when_bitcoin_cash)
            }
        }
    }

    override fun onTotalBalanceUpdated(balance: String) {
        textview_balance.text = balance
    }

    private fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_container.isRefreshing = showRefreshing
    }

    private fun onContentLoaded() {
        setShowRefreshing(false)
        no_transaction_include.gone()
    }

    private fun startReceiveFragmentEth() {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(MainActivity.ACTION_RECEIVE_ETH))
        }
    }

    private fun startReceiveFragmentBch() {
        activity?.run {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(MainActivity.ACTION_RECEIVE_BCH))
        }
    }

    override fun updateCurrencyType(isBtc: Boolean, btcFormat: Int) {
//        balanceAdapter?.onViewFormatUpdated(isBtc, btcFormat)
//        accountsAdapter?.notifyBtcChanged(isBtc, btcFormat)
    }

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        val bundle = Bundle()
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, correctedPosition)
        TransactionDetailActivity.start(activity, bundle)
    }

    /*
    Toggle between fiat - crypto currency
     */
    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    /*
    Called from MainActivity
     */
    override fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        currency_header?.setCurrentlySelectedCurrency(cryptoCurrency)
    }

    /**
     * Called from MainActivity
     * Position is offset to account for first item being "All Wallets". If returned result is -1,
     * [SendFragment] and [ReceiveFragment] can safely ignore and choose the defaults
     * instead.
     */
    fun getSelectedAccountPosition(): Int {
        var position = accounts_spinner.selectedItemPosition
        if (position >= accounts_spinner.count - 1) {
            // End of list is imported addresses, ignore
            position = 0
        }

        return position - 1
    }

    companion object {

        const val ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH"
        const val KEY_TRANSACTION_LIST_POSITION = "transaction_list_position"
        const val KEY_TRANSACTION_HASH = "transaction_hash"
        private const val ARGUMENT_BROADCASTING_PAYMENT = "broadcasting_payment"

        @JvmStatic
        fun newInstance(broadcastingPayment: Boolean): BalanceFragment {
            return BalanceFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment)
                }
            }
        }

    }

    interface OnFragmentInteractionListener {
        fun resetNavigationDrawer()
    }
}