package piuk.blockchain.android.ui.auth

import android.content.Context
import android.support.v7.app.AlertDialog
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.ui.account.AccountPresenter
import piuk.blockchain.android.util.AppRate
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.extensions.toast

internal class EnvironmentSwitcher(private val context: Context, private val prefsUtil: PrefsUtil) {

    fun showDebugMenu() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle("Debug settings")
                .setMessage("Select 'Reset Prefs' to reset various device timers and saved states, such as warning dialogs, onboarding etc.\n\nSelect 'Wipe Wallet' to log out and completely reset this app.")
                .setPositiveButton("Reset Prefs") { _, _ -> resetPrefs() }
                .setNegativeButton("Reset Wallet") { _, _ -> AppUtil(context).clearCredentialsAndRestart() }
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show()
    }

    private fun resetPrefs() {
        with(prefsUtil) {
            removeValue(PrefsUtil.KEY_PIN_FAILS)
            removeValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED)
            removeValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER)
            removeValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER)
            removeValue(AccountPresenter.KEY_WARN_TRANSFER_ALL)
            removeValue(PrefsUtil.KEY_APP_VISITS)
            removeValue(PrefsUtil.KEY_ONBOARDING_COMPLETE)
            removeValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN)
            removeValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED)
            removeValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE)
        }

        AppRate.reset(context)
        AccessState.getInstance().pin = null

        context.toast("Prefs Reset")
    }

}
