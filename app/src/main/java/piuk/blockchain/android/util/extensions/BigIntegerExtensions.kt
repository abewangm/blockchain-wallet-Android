package piuk.blockchain.android.util.extensions

import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger

fun BigInteger.getAmountRangeBtc(): String {
    val satoshis = 100_000_000L

    return when ((toLong() / satoshis)) {
        in 0.0..0.05 -> "0 - 0.05 BTC"
        in 0.05..0.1 -> "0.05 - 0.1 BTC"
        in 0.1..0.5 -> "0.1 - 0.5 BTC"
        in 0.5..1.0 -> "0.5 - 1.0 BTC"
        in 1.0..10.0 -> "1.0 - 10 BTC"
        in 10.0..100.0 -> "10 - 100 BTC"
        in 100.0..1_000.0 -> "100 - 1000 BTC"
        else -> "> 1000 BTC"
    }
}

fun BigInteger.getAmountRangeBch(): String {
    val satoshis = 100_000_000L

    return when ((toLong() / satoshis)) {
        in 0.0..0.05 -> "0 - 0.05 BCH"
        in 0.05..0.1 -> "0.05 - 0.1 BCH"
        in 0.1..0.5 -> "0.1 - 0.5 BCH"
        in 0.5..1.0 -> "0.5 - 1.0 BCH"
        in 1.0..10.0 -> "1.0 - 10 BCH"
        in 10.0..100.0 -> "10 - 100 BCH"
        in 100.0..1_000.0 -> "100 - 1000 BCH"
        else -> "> 1000 BCH"
    }
}

fun BigInteger.getAmountRangeEth(): String {
    val amountEth = Convert.fromWei(BigDecimal(this), Convert.Unit.ETHER)

    return when (amountEth.toLong()) {
        in 0.0..0.05 -> "0 - 0.05 ETH"
        in 0.05..0.1 -> "0.05 - 0.1 ETH"
        in 0.1..0.5 -> "0.1 - 0.5 ETH"
        in 0.5..1.0 -> "0.5 - 1.0 ETH"
        in 1.0..10.0 -> "1.0 - 10 ETH"
        in 10.0..100.0 -> "10 - 100 ETH"
        in 100.0..1_000.0 -> "100 - 1000 ETH"
        else -> "> 1000 ETH"
    }
}