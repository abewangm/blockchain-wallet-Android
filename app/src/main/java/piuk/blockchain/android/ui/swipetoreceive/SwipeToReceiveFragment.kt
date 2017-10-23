package piuk.blockchain.android.ui.swipetoreceive

import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_swipe_to_receive.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.util.extensions.*
import javax.inject.Inject

class SwipeToReceiveFragment : BaseFragment<SwipeToReceiveView, SwipeToReceivePresenter>(), SwipeToReceiveView {

    @Inject lateinit var swipeToReceivePresenter: SwipeToReceivePresenter
    override val cryptoCurrency: CryptoCurrencies
        get() = arguments.getSerializable(ARGUMENT_CRYPTOCURRENCY) as CryptoCurrencies

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT) {
                // Update UI with new Address + QR
                presenter?.onViewReady()
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_swipe_to_receive)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageview_qr.setOnClickListener { showClipboardWarning() }
        textview_address.setOnClickListener { showClipboardWarning() }

        onViewReady()
    }

    override fun displayReceiveAddress(address: String) {
        textview_address.text = address

        // Register address as the one we're interested in via broadcast
        val intent = Intent(WebSocketService.ACTION_INTENT).apply { putExtra("address", address) }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        // Listen for corresponding broadcasts
        LocalBroadcastManager.getInstance(context).registerReceiver(
                broadcastReceiver, IntentFilter(BalanceFragment.ACTION_INTENT))
    }

    override fun displayReceiveAccount(accountName: String) {
        textview_account?.text = accountName
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.LOADING -> displayLoading()
            UiState.CONTENT -> showContent()
            UiState.FAILURE -> showNoAddressesAvailable()
            UiState.EMPTY -> showNoAddressesAvailable()
        }
    }

    override fun displayQrCode(bitmap: Bitmap) {
        imageview_qr.setImageBitmap(bitmap)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
    }

    override fun createPresenter() = swipeToReceivePresenter

    override fun getMvpView() = this

    private fun showContent() {
        progress_bar.gone()
        imageview_qr.visible()
    }

    private fun displayLoading() {
        progress_bar.visible()
        imageview_qr.invisible()
    }

    private fun showNoAddressesAvailable() {
        layout_content.gone()
        layout_error.visible()
    }

    private fun showClipboardWarning() {
        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, { _, _ ->
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", textview_address.text)
                    toast(R.string.copied_to_clipboard)
                    clipboard.primaryClip = clip
                })
                .setNegativeButton(R.string.no, null)
                .show()
    }

    companion object {

        private const val ARGUMENT_CRYPTOCURRENCY = "ARGUMENT_CRYPTOCURRENCY"

        @JvmStatic
        fun newInstance(cryptoCurrency: CryptoCurrencies): SwipeToReceiveFragment =
                SwipeToReceiveFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARGUMENT_CRYPTOCURRENCY, cryptoCurrency)
                    }
                }
    }

}
