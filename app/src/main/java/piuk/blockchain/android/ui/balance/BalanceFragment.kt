package piuk.blockchain.android.ui.balance


import android.app.Activity
import android.content.*
import android.content.pm.ShortcutManager
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatSpinner
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_balance.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.LegacyBalanceFragment.*
import piuk.blockchain.android.ui.balance.adapter.BalanceAdapter
import piuk.blockchain.android.ui.balance.adapter.BalanceListClickListener
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.MainActivity.ACCOUNT_EDIT
import piuk.blockchain.android.ui.receive.ReceiveFragment
import piuk.blockchain.android.ui.send.SendFragment
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.OnItemSelectedListener
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.*
import java.util.*

class BalanceFragment : BaseFragment<BalanceView, BalancePresenter>(), BalanceView, BalanceListClickListener {

    private var progressDialog: MaterialProgressDialog? = null
    private var accountsAdapter: BalanceHeaderAdapter? = null
    private var interactionListener: OnFragmentInteractionListener? = null
    private var balanceAdapter: BalanceAdapter? = null
    private var spacerDecoration: BottomSpacerDecoration? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_INTENT && activity != null) {
                presenter.onViewReady()
                recyclerview.scrollToPosition(0)
            }
        }
    }

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

        textview_balance.setOnClickListener { presenter.invertViewType() }

        onViewReady()
    }

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        goToTransactionDetail(correctedPosition)
    }

    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun onFctxClicked(fctxId: String) {
        presenter.onPendingTransactionClicked(fctxId)
    }

    override fun onFctxLongClicked(fctxId: String) {
        presenter.onPendingTransactionLongClicked(fctxId)
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
            monetaryUtil: MonetaryUtil,
            isBtc: Boolean
    ) {
        if (accountsAdapter == null) {
            accountsAdapter = BalanceHeaderAdapter(
                    context,
                    R.layout.spinner_balance_header,
                    accounts,
                    isBtc,
                    monetaryUtil,
                    fiat,
                    lastPrice
            ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }

            accounts_spinner.adapter = accountsAdapter
        }

        if (accounts.size > 1) {
            accounts_spinner.visible()
        } else if (accounts.isNotEmpty()) {
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
            scrollToPosition(0)
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }

        generateLauncherShortcuts()
    }

    override fun onContactsHashMapUpdated(
            contactsTransactionMap: HashMap<String, String>,
            notesTransactionMap: HashMap<String, String>
    ) {
        balanceAdapter?.onContactsMapChanged(contactsTransactionMap, notesTransactionMap)
    }

    override fun onExchangeRateUpdated(exchangeRate: Double, isBtc: Boolean) {
        if (balanceAdapter == null) {
            setUpRecyclerView(exchangeRate, isBtc)
        } else {
            balanceAdapter?.onPriceUpdated(exchangeRate)
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
        }

        LocalBroadcastManager.getInstance(context)
                .registerReceiver(receiver, IntentFilter(ACTION_INTENT))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == ACCOUNT_EDIT) {
            // Potentially an Account has been archived - reload all data
            onViewReady()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showFctxRequiringAttention(number: Int) {
        (activity as MainActivity).setMessagesCount(number)
    }

    override fun showToast(message: Int, toastType: String) {
        context.toast(message, toastType)
    }

    override fun showSendAddressDialog(fctxId: String) {
        showDialog(
                R.string.contacts_send_address_message,
                DialogInterface.OnClickListener { _, _ -> presenter.onAccountChosen(0, fctxId) },
                true
        )
    }

    override fun showWaitingForPaymentDialog() =
            showDialog(R.string.contacts_waiting_for_payment_message, null, false)

    override fun showWaitingForAddressDialog() =
            showDialog(R.string.contacts_waiting_for_address_message, null, false)

    override fun showTransactionDeclineDialog(fctxId: String) = showDialog(
            R.string.contacts_decline_pending_transaction,
            DialogInterface.OnClickListener { _, _ -> presenter.confirmDeclineTransaction(fctxId) },
            true
    )

    override fun showTransactionCancelDialog(fctxId: String) = showDialog(
            R.string.contacts_cancel_pending_transaction,
            DialogInterface.OnClickListener { _, _ -> presenter.confirmCancelTransaction(fctxId) },
            true
    )

    override fun showAccountChoiceDialog(accounts: List<String>, fctxId: String) {
        val spinner = AppCompatSpinner(activity)
        spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, accounts)
        val selection = intArrayOf(0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selection[0] = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }

        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_choose_account_message)
                .setView(ViewUtils.getAlertDialogPaddedView(context, spinner))
                .setPositiveButton(android.R.string.ok, { _, _ -> presenter.onAccountChosen(selection[0], fctxId) })
                .create()
                .show()
    }

    override fun initiatePayment(uri: String, recipientId: String, mdid: String, fctxId: String) {
        interactionListener?.onPaymentInitiated(uri, recipientId, mdid, fctxId)
    }

    override fun startBuyActivity() {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(MainActivity.ACTION_BUY))
    }

    override fun startReceiveFragment() {
        LocalBroadcastManager.getInstance(activity).sendBroadcast(Intent(MainActivity.ACTION_RECEIVE))
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

    /**
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

    private fun setUpRecyclerView(exchangeRate: Double, isBtc: Boolean) {
        balanceAdapter = BalanceAdapter(
                activity,
                exchangeRate,
                isBtc,
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

    private fun generateLauncherShortcuts() {
        if (AndroidUtils.is25orHigher() && presenter.areLauncherShortcutsEnabled()) {
            val launcherShortcutHelper = LauncherShortcutHelper(
                    activity,
                    presenter.payloadDataManager,
                    activity.getSystemService(ShortcutManager::class.java))

            launcherShortcutHelper.generateReceiveShortcuts()
        }
    }

    private fun showDialog(
            @StringRes message: Int,
            clickListener: DialogInterface.OnClickListener?,
            showNegativeButton: Boolean
    ) {
        val builder = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setNegativeButton(android.R.string.cancel, null)

        if (showNegativeButton) {
            builder.setNegativeButton(android.R.string.cancel, null)
        }
        builder.show()
    }

    companion object {

        @JvmStatic
        fun newInstance(broadcastingPayment: Boolean): BalanceFragment {
            val args = Bundle().apply { putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment) }
            return BalanceFragment().apply { arguments = args }
        }

    }

    interface OnFragmentInteractionListener {

        fun resetNavigationDrawer()

        fun onPaymentInitiated(uri: String, recipientId: String, mdid: String, fctxId: String)

    }

}