package piuk.blockchain.android.util.extensions

import java.math.BigInteger

fun BigInteger.getAmountRange(): String {
    val SATOSHIS = 100_000_000L

    return when ((toLong() / SATOSHIS)) {
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