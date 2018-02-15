package piuk.blockchain.android.data.api

import info.blockchain.wallet.api.Environment
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinCashTestNet3Params
import org.bitcoinj.params.BitcoinMainNetParams
import org.bitcoinj.params.BitcoinTestNet3Params
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.util.annotations.Mockable

@Mockable
class EnvironmentSettings {

    fun shouldShowDebugMenu(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD
    }

    val environment: Environment = Environment.fromString(BuildConfig.ENVIRONMENT)

    val explorerUrl: String = BuildConfig.EXPLORER_URL

    val apiUrl: String = BuildConfig.API_URL

    val btcWebsocketUrl: String = BuildConfig.BITCOIN_WEBSOCKET_URL

    val ethWebsocketUrl: String = BuildConfig.ETHEREUM_WEBSOCKET_URL

    val bchWebsocketUrl: String = BuildConfig.BITCOIN_CASH_WEBSOCKET_URL

    val bitcoinNetworkParameters: NetworkParameters
        get() = when (environment) {
            Environment.TESTNET -> BitcoinTestNet3Params.get()
            else -> BitcoinMainNetParams.get()
        }

    val bitcoinCashNetworkParameters: NetworkParameters
        get() = when (environment) {
            Environment.TESTNET -> BitcoinCashTestNet3Params.get()
            else -> BitcoinCashMainNetParams.get()
        }
}
