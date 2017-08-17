package piuk.blockchain.android.data.preference

import piuk.blockchain.android.util.PrefsUtil

object CurrencyState {

    enum class Currency(val type: Int) {
        BTC(1), FIAT(2), ETHER(3)
    }

    lateinit var prefs: PrefsUtil

    fun init(prefs: PrefsUtil) {
        this.prefs = prefs
    }

    fun currencyDisplayState(): Int {
        return prefs.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, Currency.BTC.type)
    }

    fun currencyDisplayState(currency: Currency) {
        prefs.setValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, currency.type)
    }

    //Temp
    fun isBtc() : Boolean {
        return currencyDisplayState() == Currency.BTC.type;
    }

    //Temp
    fun setIsBtc(isBtc : Boolean) {
        if(isBtc) {
            currencyDisplayState(Currency.BTC)
        } else {
            currencyDisplayState(Currency.FIAT)
        }
    }
}
