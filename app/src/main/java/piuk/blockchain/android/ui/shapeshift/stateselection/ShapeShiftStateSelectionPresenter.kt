package piuk.blockchain.android.ui.shapeshift.stateselection

import android.app.Activity
import piuk.blockchain.android.R
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import javax.inject.Inject

@Mockable
class ShapeShiftStateSelectionPresenter @Inject constructor(
        private val walletOptionsDataManager: WalletOptionsDataManager
) : BasePresenter<ShapeShiftStateSelectionView>() {

    override fun onViewReady() {
    }

    fun updateAmericanState(state: String) {

        walletOptionsDataManager.isStateWhitelisted(state)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(
                        {
                            if (it) {
                                walletOptionsDataManager.setAmericanStateSelectionRequired(false)
                                walletOptionsDataManager.setAmericanState(state)
                                view.finishActivityWithResult(Activity.RESULT_OK)
                            } else {
                                view.onError(R.string.shapeshift_unavailable_in_state)
                            }
                        },
                        {
                            Timber.e(it)
                            view.finishActivityWithResult(Activity.RESULT_CANCELED)
                        })
    }
}
