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
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume
import javax.inject.Inject

class ShapeShiftActivity : BaseMvpActivity<ShapeShiftView, ShapeShiftPresenter>(), ShapeShiftView {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var shapeshiftPresenter: ShapeShiftPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        shapeshift_retry_button.setOnClickListener { presenter.onRetryPressed() }

        shapeshift_recycler_view.adapter = TradesAdapter(this)
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
        shapeshift_loading_layout.gone()
        shapeshift_error_layout.gone()
        shapeshift_recycler_view.visible()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ShapeShiftActivity::class.java))
        }

    }

}