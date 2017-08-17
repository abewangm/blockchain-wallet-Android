package piuk.blockchain.android.data.access

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.ui.auth.LogoutActivity
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.util.PrefsUtil


object AccessState {

    val LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT"

    private val LOGOUT_TIMEOUT_MILLIS = 1000L * 30L

    private lateinit var prefs: PrefsUtil
    private var rxBus: RxBus? = null

    var pin: String? = null
    private var logoutPendingIntent: PendingIntent? = null
    var isLoggedIn = false
        set(loggedIn) {
            logIn()
            field = loggedIn
            if (this.isLoggedIn) {
                rxBus!!.emitEvent(AuthEvent::class.java, AuthEvent.LOGIN)
            } else {
                rxBus!!.emitEvent(AuthEvent::class.java, AuthEvent.LOGOUT)
            }
        }
    private var canAutoLogout = true

    fun initAccessState(context: Context, prefs: PrefsUtil, rxBus: RxBus) {
        this.prefs = prefs
        this.rxBus = rxBus

        val intent = Intent(context, LogoutActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = AccessState.LOGOUT_ACTION
        logoutPendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
    }

    /**
     * Called from [BaseAuthActivity.onPause]
     */
    fun startLogoutTimer(context: Context) {
        if (canAutoLogout) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS, logoutPendingIntent)
        }
    }

    /**
     * Called from [BaseAuthActivity.onResume]
     */
    fun stopLogoutTimer(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(logoutPendingIntent)
    }

    fun logout(context: Context) {
        pin = null
        val intent = Intent(context, LogoutActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = LOGOUT_ACTION
        context.startActivity(intent)
    }

    fun unpairWallet() {
        rxBus!!.emitEvent(AuthEvent::class.java, AuthEvent.UNPAIR)
    }

    fun disableAutoLogout() {
        canAutoLogout = false
    }

    fun enableAutoLogout() {
        canAutoLogout = true
    }

    /**
     * Clears everything but the GUID for logging back in
     */
    fun logOut() {
        val guid = prefs.getValue(PrefsUtil.KEY_GUID, "")
        prefs.clear()

        prefs.setValue(PrefsUtil.LOGGED_OUT, true)
        prefs.setValue(PrefsUtil.KEY_GUID, guid)
    }

    /**
     * Reset value once user logged in
     */
    fun logIn() {
        prefs.setValue(PrefsUtil.LOGGED_OUT, false)
    }
}
