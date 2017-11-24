package piuk.blockchain.android.data.currency


enum class CryptoCurrencies(val symbol: String, val unit: String) {
    BTC("BTC", "Bitcoin"),
    ETHER("ETH", "Ether"),
    BCH("BCH", "Bitcoin Cash");

    companion object {
        fun fromString(text: String): CryptoCurrencies? {
            for (status in CryptoCurrencies.values()) {
                if (status.symbol.equals(text, ignoreCase = true)) {
                    return status
                }
            }
            return null
        }
    }
}
