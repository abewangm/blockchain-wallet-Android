package piuk.blockchain.android.data.currency;

import piuk.blockchain.android.util.PrefsUtil;

public class CurrencyState {

    private static CurrencyState instance;

    private PrefsUtil prefs;
    private CryptoCurrencies cryptoCurrency;
    private boolean isDisplayingCryptoCurrency;

    private CurrencyState() {
        isDisplayingCryptoCurrency = false;
    }

    public static CurrencyState getInstance() {
        if (instance == null)
            instance = new CurrencyState();
        return instance;
    }

    public void init(PrefsUtil prefs) {
        this.prefs = prefs;
        String value = prefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name());
        cryptoCurrency = CryptoCurrencies.valueOf(value);
        isDisplayingCryptoCurrency = true;
    }

    public CryptoCurrencies getCryptoCurrency() {
        return cryptoCurrency;
    }

    public void setCryptoCurrency(CryptoCurrencies cryptoCurrency) {
        prefs.setValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, cryptoCurrency.name());
        this.cryptoCurrency = cryptoCurrency;
    }

    public void toggleCryptoCurrency() {
        if (cryptoCurrency == CryptoCurrencies.BTC) {
            cryptoCurrency = CryptoCurrencies.ETHER;
        } else {
            cryptoCurrency = CryptoCurrencies.BTC;
        }

        setCryptoCurrency(cryptoCurrency);

    }

    public void toggleDisplayingCrypto() {
        isDisplayingCryptoCurrency = !isDisplayingCryptoCurrency;
    }

    public boolean isDisplayingCryptoCurrency() {
        return isDisplayingCryptoCurrency;
    }

    public void setDisplayingCryptoCurrency(boolean displayingCryptoCurrency) {
        isDisplayingCryptoCurrency = displayingCryptoCurrency;
    }

    public String getFiatUnit() {
        return prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }
}
