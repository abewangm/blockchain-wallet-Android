package piuk.blockchain.android.ui.shapeshift.inprogress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.util.helperfunctions.consume
import javax.inject.Inject

class TradeInProgressActivity : BaseMvpActivity<TradeInProgressView, TradeInProgressPresenter>(), TradeInProgressView {

    @Suppress("MemberVisibilityCanPrivate", "unused")
    @Inject lateinit var tradeInProgressPresenter: TradeInProgressPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_in_progress)
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = tradeInProgressPresenter

    override fun getView() = this

    companion object {

        // TODO: Pass parcelable object
        fun start(context: Context) {
            context.startActivity(Intent(context, TradeInProgressActivity::class.java))
        }

    }

}