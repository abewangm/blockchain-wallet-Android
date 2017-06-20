package piuk.blockchain.android.ui.balance


import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_balance.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.LegacyBalanceFragment.KEY_TRANSACTION_LIST_POSITION
import piuk.blockchain.android.ui.balance.adapter.BalanceAdapter
import piuk.blockchain.android.ui.balance.adapter.BalanceListClickListener
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.OnItemSelectedListener
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.*

class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView, BalanceListClickListener {

    private var progressDialog: MaterialProgressDialog? = null
    private var accountsAdapter: BalanceHeaderAdapter? = null
    private var interactionListener: OnFragmentInteractionListener? = null
    private var balanceAdapter: BalanceAdapter? = null
    private var spacerDecoration: BottomSpacerDecoration? = null

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = container!!.inflate(R.layout.fragment_balance)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
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

        onViewReady()
    }

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        goToTransactionDetail(correctedPosition)
    }

    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun onFctxClicked(fctxId: String) {
//        viewModel.onPendingTransactionClicked(fctxId)
    }

    override fun onFctxLongClicked(fctxId: String) {
//        viewModel.onPendingTransactionLongClicked(fctxId)
    }

    override fun onViewTypeChanged(isBtc: Boolean) {
        balanceAdapter?.onViewFormatUpdated(isBtc)
        accountsAdapter?.notifyBtcChanged(isBtc)
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.FAILURE, UiState.EMPTY -> onEmptyState()
            UiState.CONTENT -> onContentLoaded()
            UiState.LOADING -> setShowRefreshing(true)
        }
    }

    override fun onAccountsUpdated(
            accounts: List<ItemAccount>,
            lastPrice: Double,
            fiat: String,
            monetaryUtil: MonetaryUtil
    ) {
        accountsAdapter = BalanceHeaderAdapter(
                context,
                R.layout.spinner_balance_header,
                accounts,
                presenter.getViewType(),
                monetaryUtil,
                fiat,
                lastPrice
        ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }

        accounts_spinner.adapter = accountsAdapter

        textview_balance.setOnClickListener { presenter.setViewType(!presenter.getViewType()) }

        if (accounts.size > 1) {
            accounts_spinner.visible()
        } else if (!accounts.isEmpty()) {
            accounts_spinner.setSelection(0, false)
            accounts_spinner.invisible()
        }
        accounts_spinner.setOnTouchListener({ _, event ->
            event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
        })

        accounts_spinner.onItemSelectedListener = OnItemSelectedListener {
            presenter.onAccountChosen(it)
            recyclerview.scrollToPosition(0)
        }
    }

    override fun onTotalBalanceUpdated(balance: String) {
        textview_balance.text = balance
    }

    override fun onTransactionsUpdated(displayObjects: List<Any>) {
        balanceAdapter?.items = displayObjects

        if (spacerDecoration == null) {
            spacerDecoration = BottomSpacerDecoration(
                    context,
                    ViewUtils.convertDpToPixel(56f, context).toInt()
            )
        }
        recyclerview.apply {
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }
    }

    override fun onExchangeRateUpdated(exchangeRate: Double) {
        if (balanceAdapter == null) {
            setUpRecyclerView(exchangeRate)
        } else {
            balanceAdapter?.onPriceUpdated(exchangeRate)
        }
    }

    override fun onPause() {
        super.onPause()
        // Fixes issue with Swipe Layout messing with Fragment transitions
        swipe_container?.let {
            swipe_container.isRefreshing = false
            swipe_container.destroyDrawingCache()
            swipe_container.clearAnimation()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        interactionListener = activity as OnFragmentInteractionListener?
    }

    override fun showToast(message: Int, toastType: String) {
        activity.toast(message, toastType)
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(activity).apply {
            setCancelable(false)
            setMessage(R.string.please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun getIfContactsEnabled(): Boolean = BuildConfig.CONTACTS_ENABLED

    override fun createPresenter(): BalancePresenter = BalancePresenter()

    override fun getMvpView(): BalanceView = this

    private fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_container.isRefreshing = showRefreshing
    }

    private fun onEmptyState() {
        setShowRefreshing(false)
        no_transaction_include.visible()
    }

    private fun onContentLoaded() {
        setShowRefreshing(false)
        no_transaction_include.gone()
    }

    private fun setUpRecyclerView(exchangeRate: Double) {
        balanceAdapter = BalanceAdapter(
                activity,
                exchangeRate,
                presenter.getViewType(),
                this
        ).apply { setHasStableIds(true) }

        val layoutManager = LinearLayoutManager(context)
        recyclerview.layoutManager = layoutManager
        recyclerview.adapter = balanceAdapter
        // Disable blinking animations in RecyclerView
        val animator = recyclerview.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun goToTransactionDetail(position: Int) {
        val bundle = Bundle()
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, position)
        TransactionDetailActivity.start(activity, bundle)
    }

    companion object {

        @JvmStatic
        fun newInstance(): BalanceFragment {
            // TODO
            return BalanceFragment()
        }

    }

    interface OnFragmentInteractionListener {

        fun resetNavigationDrawer()

        fun onPaymentInitiated(uri: String, recipientId: String, mdid: String, fctxId: String)

    }

}