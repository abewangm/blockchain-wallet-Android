package piuk.blockchain.android.data.walletoptions

import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.ReplaySubject
import piuk.blockchain.android.data.auth.AuthDataManager
import piuk.blockchain.android.data.settings.SettingsDataManager
import java.util.*

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

                    val lcid = Locale.getDefault().language + "-" + Locale.getDefault().country
                    val language = Locale.getDefault().language

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

    fun isShapeshiftAllowed(options: WalletOptions, settings: Settings): Boolean {

        return options.androidFlags.getOrDefault(SHOW_SHAPESHIFT, false)
                &&
                !options.shapeshift.countriesBlacklist.contains(settings.countryCode)
                //||
                //options.shapeshift.statesWhitelist TODO Where do we get State ???
    }

    companion object {
        private val SHOW_SHAPESHIFT = "showShapeshift"
    }
}
