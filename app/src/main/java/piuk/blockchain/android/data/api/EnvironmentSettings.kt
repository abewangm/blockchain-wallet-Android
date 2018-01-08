package piuk.blockchain.android.data.api

import info.blockchain.wallet.api.Environment
import org.bitcoinj.params.*

import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class EnvironmentSettings {

    fun shouldShowDebugMenu(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD
    }

    val environment: Environment
        get() = Environment.fromString(BuildConfig.ENVIRONMENT)

    val explorerUrl: String
        get() = BuildConfig.EXPLORER_URL

    val apiUrl: String
        get() = BuildConfig.API_URL

    val btcWebsocketUrl: String
        get() = BuildConfig.BITCOIN_WEBSOCKET_URL

    val ethWebsocketUrl: String
        get() = BuildConfig.ETHEREUM_WEBSOCKET_URL

    val networkParameters: AbstractNetParams
        get() {
            return when (environment) {
                Environment.TESTNET -> BitcoinTestNet3Params.get()
                else -> BitcoinMainNetParams.get()
            }
        }

    val bitcoinCashNetworkParameters: AbstractNetParams
        get() {
            return when (environment) {
                Environment.TESTNET -> BitcoinCashTestNet3Params.get()
                else -> BitcoinCashMainNetParams.get()
            }
        }
}
