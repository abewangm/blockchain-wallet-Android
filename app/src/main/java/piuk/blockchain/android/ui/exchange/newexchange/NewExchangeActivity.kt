package piuk.blockchain.android.ui.exchange.newexchange

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import javax.inject.Inject

class NewExchangeActivity : BaseMvpActivity<NewExchangeView, NewExchangePresenter>(), NewExchangeView {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var newExchangePresenter: NewExchangePresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_exchange)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        onViewReady()
    }

    override fun createPresenter() = newExchangePresenter

    override fun getView() = this

    companion object {

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, NewExchangeActivity::class.java))
        }

    }

}