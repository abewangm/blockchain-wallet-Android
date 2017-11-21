package piuk.blockchain.android.ui.shapeshift.overview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_shapeshift.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.shapeshift.newexchange.NewExchangeActivity
import piuk.blockchain.android.ui.shapeshift.overview.adapter.TradesAdapter
import piuk.blockchain.android.ui.shapeshift.overview.adapter.TradesListClickListener
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume
import timber.log.Timber
import javax.inject.Inject

class ShapeShiftActivity : BaseMvpActivity<ShapeShiftView, ShapeShiftPresenter>(), ShapeShiftView, TradesListClickListener {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var shapeshiftPresenter: ShapeShiftPresenter

    private var tradesAdapter: TradesAdapter? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        shapeshift_retry_button.setOnClickListener { presenter.onRetryPressed() }

        //TODO("Exchange rates. Crypto/FIAT")
        tradesAdapter = TradesAdapter(
                this,
                100.00,
                100.00,
                true,
                this
        )

        shapeshift_recycler_view.adapter = tradesAdapter
        shapeshift_recycler_view.layoutManager = LinearLayoutManager(this)

        onViewReady()
    }

    override fun onStateUpdated(shapeshiftState: ShapeShiftState) = when (shapeshiftState) {
        is ShapeShiftState.Data -> onData(shapeshiftState)
        is ShapeShiftState.Empty -> onEmptyLayout()
        is ShapeShiftState.Error -> onError()
        is ShapeShiftState.Loading -> onLoading()
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = shapeshiftPresenter

    override fun getView() = this

    private fun onEmptyLayout() {
        onLoading()
        NewExchangeActivity.start(this)
        // Remove self from back stack
        finish()
    }

    private fun onError() {
        shapeshift_loading_layout.gone()
        shapeshift_error_layout.visible()
        shapeshift_recycler_view.gone()
    }

    private fun onLoading() {
        shapeshift_loading_layout.visible()
        shapeshift_error_layout.gone()
        shapeshift_recycler_view.gone()
    }

    private fun onData(data: ShapeShiftState.Data) {
        //TODO("Pass trade data")
//        tradesAdapter?.items = data?.trades
        Timber.d("vos onData: "+shapeshift_recycler_view.adapter.itemCount)

        shapeshift_loading_layout.gone()
        shapeshift_error_layout.gone()
        shapeshift_recycler_view.visible()
    }

    override fun onTradeClicked(correctedPosition: Int, absolutePosition: Int) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onValueClicked(isBtc: Boolean) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ShapeShiftActivity::class.java))
        }

    }

}