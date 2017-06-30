package piuk.blockchain.android.ui.backup

import android.support.annotation.VisibleForTesting
import piuk.blockchain.android.R
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.backup.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.PrefsUtil
import timber.log.Timber
import javax.inject.Inject

class BackupVerifyPresenter : BasePresenter<BackupVerifyView>() {

    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var prefsUtil: PrefsUtil
    @Inject lateinit var backupWalletUtil: BackupWalletUtil

    private val sequence by lazy(LazyThreadSafetyMode.NONE) { getBackupConfirmSequence() }

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        view.showWordHints(listOf(sequence[0].first, sequence[1].first, sequence[2].first))
    }

    internal fun onVerifyClicked(firstWord: String, secondWord: String, thirdWord: String) {
        if (firstWord.trim { it <= ' ' }.equals(sequence[0].second, ignoreCase = true)
                && secondWord.trim { it <= ' ' }.equals(sequence[1].second, ignoreCase = true)
                && thirdWord.trim { it <= ' ' }.equals(sequence[2].second, ignoreCase = true)) {

            updateBackupStatus()
        } else {
            view.showToast(R.string.backup_word_mismatch, ToastCustom.TYPE_ERROR)
        }
    }

    @VisibleForTesting
    internal fun updateBackupStatus() {
        payloadDataManager.wallet.hdWallets[0].isMnemonicVerified = true

        compositeDisposable.add(
                payloadDataManager.syncPayloadWithServer()
                        .doOnSubscribe { view.showProgressDialog() }
                        .doAfterTerminate { view.hideProgressDialog() }
                        .subscribe({
                            prefsUtil.setValue(
                                    BackupWalletActivity.BACKUP_DATE_KEY,
                                    (System.currentTimeMillis() / 1000).toInt()
                            )
                            view.showToast(R.string.backup_confirmed, ToastCustom.TYPE_OK)
                            view.showCompletedFragment()
                        }) { throwable ->
                            Timber.e(throwable)
                            view.showToast(R.string.api_fail, ToastCustom.TYPE_ERROR)
                            view.showStartingFragment()
                        })
    }

    private fun getBackupConfirmSequence(): List<Pair<Int, String>> {
        val bundle = view.getPageBundle()
        val secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)
        return backupWalletUtil.getConfirmSequence(secondPassword)
    }

}
