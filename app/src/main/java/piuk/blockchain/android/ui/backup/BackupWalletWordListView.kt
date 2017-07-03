package piuk.blockchain.android.ui.backup

import android.os.Bundle
import piuk.blockchain.android.ui.base.View

interface BackupWalletWordListView : View {

    fun getPageBundle(): Bundle?

    fun finish()

}