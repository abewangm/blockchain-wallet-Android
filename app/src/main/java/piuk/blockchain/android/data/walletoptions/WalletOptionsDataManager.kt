package piuk.blockchain.android.data.walletoptions

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.ReplaySubject
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.settings.SettingsDataManager
import piuk.blockchain.android.util.annotations.Mockable
import java.util.*

@Mockable
class WalletOptionsDataManager(private val authDataManager: AuthDataManager,
                               private val walletOptionsState: WalletOptionsState,
                               private val settingsDataManager: SettingsDataManager) {

    /**
     * ReplaySubjects will re-emit items it observed.
     * It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    private fun initReplaySubjects() {
        val walletOptionsStream = authDataManager.walletOptions
        walletOptionsStream.subscribeWith<ReplaySubject<WalletOptions>>(walletOptionsState.walletOptionsSource)

        val walletSettingsStream = settingsDataManager.settings
        walletSettingsStream.subscribeWith<ReplaySubject<Settings>>(walletOptionsState.walletSettingsSource)
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

    /**
     * Mobile notice retrieved from wallet-options.json based on wallet setting
     * This notice will be shown on each session.
     * @param mobileNotice
     */
    fun getMobileNotice(): Observable<String> {
        initReplaySubjects()

        return walletOptionsState.walletOptionsSource.flatMap { options ->

            var result = ""

            options.mobileNotice.apply {

                if (isNotEmpty()) {

                    val lcid = authDataManager.locale.language + "-" + authDataManager.locale.country
                    val language = authDataManager.locale.language

                    if (containsKey(language)) {
                        result = get(language) ?: ""
                    } else if (containsKey(lcid)) {
                        //Regional
                        result = get(lcid) ?: ""
                    } else {
                        //Default
                        result = get("en") ?: ""
                    }
                }
            }
            return@flatMap Observable.just(result)
        }
    }

    fun showShapeshift(): Observable<Boolean> {
        initReplaySubjects()

        return Observable.zip(walletOptionsState.walletOptionsSource, walletOptionsState.walletSettingsSource,
                BiFunction({
                    options, settings -> isShapeshiftAllowed(options, settings)
                }))
    }

    private fun isShapeshiftAllowed(options: WalletOptions, settings: Settings): Boolean {

        val isShapeShiftAllowed = options.androidFlags.let { it?.get(SHOW_SHAPESHIFT)?: false }
        val blacklistedCountry = options.shapeshift.countriesBlacklist.let { it?.contains(settings.countryCode)?: false }
        val whitelistedState = options.shapeshift.statesWhitelist.let { it?.contains(settings.state)?: true }
        val isUSABlacklisted = options.shapeshift.countriesBlacklist.let { it?.contains("US")?: false }
        val isUS = settings.countryCode.equals("US")

        return isShapeShiftAllowed
                &&
                !blacklistedCountry
                &&
                (!isUS || (!isUSABlacklisted && whitelistedState))
    }

    companion object {
        private val SHOW_SHAPESHIFT = "showShapeshift"
        private val REPLAY_PROTECTION = "replayProtection"
    }

}
