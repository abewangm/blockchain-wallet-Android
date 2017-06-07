package piuk.blockchain.android.ui.launcher

import android.content.Intent
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
interface LauncherView: View {

    fun getPageIntent(): Intent

    fun onNoGuid()

    fun onRequestPin()

    fun onCorruptPayload()

    fun onRequestUpgrade()

    fun onStartMainActivity()

    fun onReEnterPassword()

    fun onStartOnboarding(emailOnly: Boolean)

}
