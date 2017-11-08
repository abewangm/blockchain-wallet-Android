package piuk.blockchain.android.data.walletoptions

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.settings.SettingsDataManager
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class WalletOptionsDataManager(
        private val authDataManager: AuthDataManager,
        private val walletOptionsState: WalletOptionsState,
        private val settingsDataManager: SettingsDataManager
) {

    /**
     * ReplaySubjects will re-emit items it observed.
     * It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    private fun initReplaySubjects() {
        val walletOptionsStream = authDataManager.walletOptions
        walletOptionsStream
                .subscribeOn(Schedulers.io())
                .subscribeWith<ReplaySubject<WalletOptions>>(walletOptionsState.walletOptionsSource)

        val walletSettingsStream = settingsDataManager.settings
        walletSettingsStream
                .subscribeOn(Schedulers.io())
                .subscribeWith<ReplaySubject<Settings>>(walletOptionsState.walletSettingsSource)
    }

    /**
     * Replay protection status retrieved from wallet-options.json.
     * If replay protection is needed, input coins will be mixed with non-replayable inputs
     * to ensure transaction is not replayable.
     */
    fun fetchReplayProtectionStatus(): Observable<Boolean> {
        initReplaySubjects()

        return walletOptionsState.walletOptionsSource.flatMap { options ->

            var result = false

            options.androidFlags.apply {

                if (isNotEmpty()) {
                    result = get(REPLAY_PROTECTION) ?: false
                }
            }
            return@flatMap Observable.just(result)
        }
    }

    fun shouldAddReplayProtection() = walletOptionsState.replayProtectionStatus

    fun setReplayProtectionStatus(status: Boolean) {
        walletOptionsState.replayProtectionStatus = status
    }

    fun showShapeshift(): Observable<Boolean> {
        initReplaySubjects()

        return Observable.zip(walletOptionsState.walletOptionsSource, walletOptionsState.walletSettingsSource,
                BiFunction({ options, settings ->
                    isShapeshiftAllowed(options, settings)
                }))
    }

    private fun isShapeshiftAllowed(options: WalletOptions, settings: Settings): Boolean {

        val isShapeShiftAllowed = options.androidFlags.let { it?.get(SHOW_SHAPESHIFT) ?: false }
        val blacklistedCountry = options.shapeshift.countriesBlacklist.let { it?.contains(settings.countryCode) ?: false }
        val whitelistedState = options.shapeshift.statesWhitelist.let { it?.contains(settings.state) ?: true }
        val isUSABlacklisted = options.shapeshift.countriesBlacklist.let { it?.contains("US") ?: false }
        val isUS = settings.countryCode == "US"

        return isShapeShiftAllowed
                && !blacklistedCountry
                && (!isUS || (!isUSABlacklisted && whitelistedState))
    }

    /**
     * Mobile info retrieved from wallet-options.json based on wallet setting
     */
    fun fetchInfoMessage(): Observable<String> {
        initReplaySubjects()

        return walletOptionsState.walletOptionsSource.flatMap { options ->

            var result = ""

            options.mobileInfo.apply {
                result = getLocalisedMessage(this)
            }
            return@flatMap Observable.just(result)
        }
    }

    /**
     * Checks to see if the client app needs to be force updated according to the wallet.options
     * JSON file. If the client is on an unsupported Android SDK, the check is bypassed to prevent
     * locking users out forever. Otherwise, an app version code ([piuk.blockchain.android.BuildConfig.VERSION_CODE])
     * less than the supplied minVersionCode will return true, and the client should be forcibly
     * upgraded.
     *
     * @param versionCode The version code of the current app
     * @param sdk The device's Android SDK version
     * @return A [Boolean] value contained within an [Observable]
     */
    fun checkForceUpgrade(versionCode: Int, sdk: Int): Observable<Boolean> {
        initReplaySubjects()

        return walletOptionsState.walletOptionsSource.flatMap {
            val androidUpgradeMap = it.androidUpgrade ?: mapOf()
            var forceUpgrade = false
            val minSdk = androidUpgradeMap["minSdk"] ?: 0
            val minVersionCode = androidUpgradeMap["minVersionCode"] ?: 0
            if (sdk < minSdk) {
                // Can safely ignore force upgrade
            } else {
                if (versionCode < minVersionCode) {
                    // Force the client to update
                    forceUpgrade = true
                }
            }

            return@flatMap Observable.just(forceUpgrade)
        }
    }

    fun getLocalisedMessage(map: Map<String, String>): String {

        var result = ""

        if (map.isNotEmpty()) {

            val lcid = authDataManager.locale.language + "-" + authDataManager.locale.country
            val language = authDataManager.locale.language

            result = when {
                map.containsKey(language) -> map[language] ?: ""
            //Regional
                map.containsKey(lcid) -> map[lcid] ?: ""
            //Default
                else -> map["en"] ?: ""
            }
        }

        return result
    }

    companion object {
        private val SHOW_SHAPESHIFT = "showShapeshift"
        private val REPLAY_PROTECTION = "replayProtection"
    }

}
