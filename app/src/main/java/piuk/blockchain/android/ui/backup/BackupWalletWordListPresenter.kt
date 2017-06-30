package piuk.blockchain.android.ui.backup

import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.backup.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.BackupWalletUtil
import javax.inject.Inject

class BackupWalletWordListPresenter : BasePresenter<BackupWalletWordListView>() {

    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var backupWalletUtil: BackupWalletUtil

    internal var secondPassword: String? = null
    private var mnemonic: List<String>? = null

    override fun onViewReady() {
        Injector.getInstance().dataManagerComponent.inject(this)

        val bundle = view.getPageBundle()
        secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)

        mnemonic = backupWalletUtil.getMnemonic(secondPassword)
        if (mnemonic == null) {
            view.finish()
        }
    }

    internal fun getWordForIndex(index: Int) = mnemonic!![index]

    internal fun getMnemonicSize() = mnemonic?.size ?: -1

}