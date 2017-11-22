package piuk.blockchain.android.ui.shapeshift.models

import android.annotation.SuppressLint
import android.os.Parcelable
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
        var xPub: String,
        // Epoch time, in milliseconds
        var expiration: Long,
        // Fee information
        var gasLimit: BigInteger,
        var feePerKb: BigInteger
) : Parcelable