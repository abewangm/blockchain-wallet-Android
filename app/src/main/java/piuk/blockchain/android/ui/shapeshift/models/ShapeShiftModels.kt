package piuk.blockchain.android.ui.shapeshift.models

import android.annotation.SuppressLint
import android.os.Parcelable
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import kotlinx.android.parcel.Parcelize
import piuk.blockchain.android.data.currency.CryptoCurrencies
import java.math.BigDecimal
import java.math.BigInteger

@SuppressLint("ParcelCreator")
@Parcelize
data class ShapeShiftData(
        val orderId: String,
        var fromCurrency: CryptoCurrencies,
        var toCurrency: CryptoCurrencies,
        // The amount you're sending to ShapeShift
        var depositAmount: BigDecimal,
        // The address you're sending the funds to
        var depositAddress: String,
        // Your change address
        var changeAddress: String,
        // The amount you'll get back from ShapeShift
        var withdrawalAmount: BigDecimal,
        // The address to which you want to receive funds
        var withdrawalAddress: String,
        // The offered exchange rate
        var exchangeRate: BigDecimal,
        // The fee to send funds to ShapeShift, in Wei or Satoshis
        var transactionFee: BigInteger,
        // The mining fee that ShapeShift takes from your new funds, *NOT* in Wei or Satoshis as this is returned from the server
        var networkFee: BigDecimal,
        // An address for if the trade fails or if there's change
        var returnAddress: String,
        // xPub for finding account
        var xPub: String,
        // Epoch time, in milliseconds
        var expiration: Long,
        // Fee information
        var gasLimit: BigInteger,
        var gasPrice: BigInteger,
        var feePerKb: BigInteger
) : Parcelable

data class TradeDetailUiState(
        @StringRes val title: Int,
        @StringRes val heading: Int,
        val message: String,
        @DrawableRes val icon: Int,
        @ColorRes val receiveColor: Int
)

data class TradeProgressUiState(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @DrawableRes val icon: Int,
        val showSteps: Boolean,
        val stepNumber: Int
)

/**
 * For strict type checking and convenience.
 */
enum class CoinPairings(val pairCode: String) {
    BTC_TO_ETH(ShapeShiftPairs.BTC_ETH),
    BTC_TO_BCH(ShapeShiftPairs.BTC_BCH),
    ETH_TO_BTC(ShapeShiftPairs.ETH_BTC),
    ETH_TO_BCH(ShapeShiftPairs.ETH_BCH),
    BCH_TO_BTC(ShapeShiftPairs.BCH_BTC),
    BCH_TO_ETH(ShapeShiftPairs.BCH_ETH);

    companion object {

        fun getPair(fromCurrency: CryptoCurrencies, toCurrency: CryptoCurrencies): CoinPairings =
                when (fromCurrency) {
                    CryptoCurrencies.BTC -> when (toCurrency) {
                        CryptoCurrencies.ETHER -> BTC_TO_ETH
                        CryptoCurrencies.BCH -> BTC_TO_BCH
                        else -> throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                    }
                    CryptoCurrencies.ETHER -> when (toCurrency) {
                        CryptoCurrencies.BTC -> ETH_TO_BTC
                        CryptoCurrencies.BCH -> ETH_TO_BCH
                        else -> throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                    }
                    CryptoCurrencies.BCH -> when (toCurrency) {
                        CryptoCurrencies.BTC -> BCH_TO_BTC
                        CryptoCurrencies.ETHER -> BCH_TO_ETH
                        else -> throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                    }
                }

    }
}