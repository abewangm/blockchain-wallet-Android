package piuk.blockchain.android.ui.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.util.extensions.inflate

class BalanceFragment2 : BaseFragment<BalanceView, BalancePresenter>(), BalanceView {

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        return container!!.inflate(R.layout.fragment_balance)
    }

    override fun createPresenter(): BalancePresenter = BalancePresenter()

    override fun getMvpView(): BalanceView = this
}