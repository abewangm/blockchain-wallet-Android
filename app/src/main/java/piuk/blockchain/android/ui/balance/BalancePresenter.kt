package piuk.blockchain.android.ui.balance

import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter

class BalancePresenter : BasePresenter<BalanceView>() {

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {

    }
}
