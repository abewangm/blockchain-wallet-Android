package piuk.blockchain.android.ui.launcher

import android.content.Intent
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.datamanagers.PayloadDataManager
import piuk.blockchain.android.data.datamanagers.SettingsDataManager
import piuk.blockchain.android.data.notifications.FcmCallbackService.EXTRA_CONTACT_ACCEPTED
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.PrefsUtil
import javax.inject.Inject


class LauncherPresenter : BasePresenter<LauncherView>() {

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    companion object {
        @JvmField val INTENT_EXTRA_VERIFIED = "verified"
    }

    @Inject lateinit var appUtil: AppUtil
    @Inject lateinit var payloadDataManager: PayloadDataManager
    @Inject lateinit var prefsUtil: PrefsUtil
    @Inject lateinit var accessState: AccessState
    @Inject lateinit var settingsDataManager: SettingsDataManager

    override fun onViewReady() {
        val intent = view.getPageIntent()
        val action = intent.action
        val scheme = intent.scheme
        val intentData = intent.dataString
        val extras = intent.extras
        val hasLoggedOut = prefsUtil.getValue(PrefsUtil.LOGGED_OUT, false)
        var isPinValidated = false

        // Store incoming bitcoin URI if needed
        if (action != null && Intent.ACTION_VIEW == action && scheme != null && scheme == "bitcoin") {
            prefsUtil.setValue(PrefsUtil.KEY_SCHEME_URL, intent.data.toString())
        }

        // Store incoming Contacts URI if needed
        if (action != null && Intent.ACTION_VIEW == action && intentData != null && intentData.contains("blockchain")) {
            prefsUtil.setValue(PrefsUtil.KEY_METADATA_URI, intentData)
        }

        // Store if coming from specific Contacts notification
        if (intent.hasExtra(EXTRA_CONTACT_ACCEPTED)) {
            prefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true)
        }

        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED)
        }

        when {
        // No GUID? Treat as new installation
            prefsUtil.getValue(PrefsUtil.KEY_GUID, "").isEmpty() -> view.onNoGuid()
        // User has logged out recently. Show password reentry page
            hasLoggedOut -> view.onReEnterPassword()
        // No PIN ID? Treat as installed app without confirmed PIN
            prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty() -> view.onRequestPin()
        // Installed app, check sanity
            !appUtil.isSane -> view.onCorruptPayload()
        // Legacy app has not been prompted for upgrade
            isPinValidated && !payloadDataManager.wallet.isUpgraded -> promptUpgrade()
        // App has been PIN validated
            isPinValidated || accessState.isLoggedIn -> checkOnboardingStatus()
        // Normal login
            else -> view.onRequestPin()
        }
    }

    private fun promptUpgrade() {
        accessState.setIsLoggedIn(true)
        view.onRequestUpgrade()
    }

    private fun checkOnboardingStatus() {
        accessState.setIsLoggedIn(true)
        if (appUtil.isNewlyCreated) {
            view.onStartOnboarding(false)
        } else {
            settingsDataManager.initSettings(
                    payloadDataManager.wallet.guid,
                    payloadDataManager.wallet.sharedKey)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe({ settings ->
                        if (!settings.isEmailVerified
                                && settings.email != null
                                && !settings.email.isEmpty()) {
                            var visits = prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)

                            // Nag user to verify email after second login
                            when (visits) {
                                1 -> view.onStartOnboarding(true)
                                else -> view.onStartMainActivity()
                            }

                            visits++
                            prefsUtil.setValue(PrefsUtil.KEY_APP_VISITS, visits)
                        } else {
                            view.onStartMainActivity()
                        }
                    }, { _ -> view.onStartMainActivity() })

        }
    }

}
