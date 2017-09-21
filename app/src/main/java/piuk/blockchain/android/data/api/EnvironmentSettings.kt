package piuk.blockchain.android.data.api

import info.blockchain.wallet.api.Environment

import org.bitcoinj.params.AbstractBitcoinNetParams
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params

import piuk.blockchain.android.BuildConfig

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

    val networkParameters: AbstractBitcoinNetParams
        get() {
            return when (environment) {
                Environment.TESTNET -> TestNet3Params.get()
                else -> MainNetParams.get()
            }
        }
}
