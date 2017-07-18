package piuk.blockchain.android.ui.backup.start

import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.ui.base.BasePresenter
import javax.inject.Inject

class BackupWalletStartingPresenter @Inject constructor(
        private val payloadDataManager: PayloadDataManager
) : BasePresenter<BackupWalletStartingView>() {

    override fun onViewReady() {
        // No-op
    }

    internal fun isDoubleEncrypted() = payloadDataManager.isDoubleEncrypted

    // TODO: Refactor the second password handler so that it can be called from here

}