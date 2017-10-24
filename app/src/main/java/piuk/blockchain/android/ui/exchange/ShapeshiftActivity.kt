package piuk.blockchain.android.ui.exchange

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
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
        // TODO: No trades, take user directly to new trade
    }

    private fun onError() {
        // TODO: Show error
    }

    private fun onLoading() {
        // TODO: Show loading
    }

    private fun onData(data: ShapeShiftState.Data) {
        // TODO: Update UI accordingly
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ShapeShiftActivity::class.java))
        }

    }

}