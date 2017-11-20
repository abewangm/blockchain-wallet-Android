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
        var depositAmount: BigDecimal,
        var withdrawalAmount: BigDecimal,
        var exchangeRate: BigDecimal,
        // Fee in Wei or Satoshis
        var transactionFee: BigInteger,
        var networkFee: BigDecimal,
        var receiveAddress: String,
        var changeAddress: String,
        var xPub: String,
        var expiration: Long,
        // Fee information
        var gasLimit: BigInteger,
        var feePerKb: BigInteger
) : Parcelable