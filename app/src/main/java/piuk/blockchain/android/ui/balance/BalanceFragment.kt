package piuk.blockchain.android.ui.balance

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_contacts.*
import kotlinx.android.synthetic.main.fragment_balance.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.OnItemSelectedListener
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.invisible
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible

class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView {

    private var progressDialog: MaterialProgressDialog? = null
    private var accountsAdapter: BalanceHeaderAdapter? = null
    private var interactionListener: OnFragmentInteractionListener? = null

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
        TODO("not implemented")
    }

    override fun onExchangeRateUpdated() {
        TODO("not implemented")
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