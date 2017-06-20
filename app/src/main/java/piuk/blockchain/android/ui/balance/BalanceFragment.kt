package piuk.blockchain.android.ui.balance

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_contacts.*
import kotlinx.android.synthetic.main.fragment_balance.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.LegacyBalanceFragment.SHOW_BTC
import piuk.blockchain.android.ui.balance.adapter.BalanceAdapter
import piuk.blockchain.android.ui.balance.adapter.BalanceListClickListener
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.OnItemSelectedListener
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.invisible
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible
import java.util.*

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
        onViewReady()
//        setShowRefreshing(true)
//        no_tx_message_layout.gone()

        val fiatString = PrefsUtil(context).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
//        val lastPrice = viewModel.getLastPrice(fiatString)
        val lastPrice = 5.0

        balanceAdapter = BalanceAdapter(
                activity,
                lastPrice,
                isBtc = true,
                listClickListener = this
        ).apply { setHasStableIds(true) }

        val layoutManager = LinearLayoutManager(context)
        recyclerview.layoutManager = layoutManager
        recyclerview.adapter = balanceAdapter
        // Disable blinking animations in RecyclerView
        val animator = recyclerview.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        swipe_container.setProgressViewEndTarget(
                false,
                ViewUtils.convertDpToPixel(72F + 20F, context).toInt()
        )
        swipe_container.setOnRefreshListener { getPresenter().onRefreshRequested() }
        swipe_container.setColorSchemeResources(
                R.color.product_green_medium,
                R.color.primary_blue_medium,
                R.color.product_red_medium
        )
    }

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        // goToTransactionDetail(correctedPosition)
    }

    override fun onValueClicked(isBtc: Boolean) {
//        isBTC = isBtc
        PrefsUtil(context).setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, if (isBtc) SHOW_BTC else SHOW_FIAT)
//        balanceAdapter.onViewFormatUpdated(isBTC)
//        updateBalanceAndTransactionList(false)
    }

    override fun onFctxClicked(fctxId: String) {
//        viewModel.onPendingTransactionClicked(fctxId)
    }

    override fun onFctxLongClicked(fctxId: String) {
//        viewModel.onPendingTransactionLongClicked(fctxId)
    }

    override fun onAccountsUpdated(
            accounts: List<ItemAccount>,
            lastPrice: Double,
            fiat: String,
            monetaryUtil: MonetaryUtil
    ) {

        if (accounts.size > 1) {
            accounts_spinner.visible()
        } else if (!accounts.isEmpty()) {
            accounts_spinner.setSelection(0)
            accounts_spinner.invisible()
        }

        accountsAdapter = BalanceHeaderAdapter(
                context,
                R.layout.spinner_balance_header,
                accounts,
                true,
                monetaryUtil,
                fiat,
                lastPrice
        ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }

        accounts_spinner.adapter = accountsAdapter
        accounts_spinner.setOnTouchListener({ _, event ->
            event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
        })

        accounts_spinner.onItemSelectedListener = OnItemSelectedListener {
            getPresenter().onAccountChosen(it)
            recyclerview.scrollToPosition(0)
        }
    }

    override fun onTotalBalanceUpdated(balance: String) {
        textview_balance.text = balance
    }

    override fun onTransactionsUpdated(displayObjects: List<Any>) {
        balanceAdapter?.items = ArrayList<Any>().apply { addAll(displayObjects) }

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

    override fun onExchangeRateUpdated() {
        TODO("not implemented, maybe want to pass the new value here to update adapter")
    }

    override fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_refresh_layout.isRefreshing = showRefreshing
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

    override fun getIfContactsEnabled(): Boolean = BuildConfig.CONTACTS_ENABLED

    override fun createPresenter(): BalancePresenter = BalancePresenter()

    override fun getMvpView(): BalanceView = this

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