package piuk.blockchain.android.ui.backup.start

import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import javax.inject.Inject

class BackupWalletStartingPresenter : BasePresenter<BackupWalletStartingView>() {

    @Inject lateinit var payloadDataManager: PayloadDataManager

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    override fun onViewReady() {
        // No-op
    }

    internal fun isDoubleEncrypted() = payloadDataManager.isDoubleEncrypted

    // TODO: Refactor the second password handler so that it can be called from here

}