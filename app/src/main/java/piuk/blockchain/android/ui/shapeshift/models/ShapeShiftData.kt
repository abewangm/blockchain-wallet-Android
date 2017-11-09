package piuk.blockchain.android.ui.shapeshift.models

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import piuk.blockchain.android.data.currency.CryptoCurrencies
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
data class ShapeShiftData(
        var fromCurrency: CryptoCurrencies,
        var toCurrency: CryptoCurrencies,
        var depositAmount: BigDecimal,
        var receiveAmount: BigDecimal,
        var exchangeRate: Double,
        var transactionFee: BigDecimal,
        var networkFee: BigDecimal,
        var receiveAddress: String,
        var changeAddress: String
) : Parcelable