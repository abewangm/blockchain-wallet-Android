package piuk.blockchain.android.util;

import info.blockchain.api.data.TickerItem;
import info.blockchain.api.exchangerates.ExchangeRates;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.exceptions.ApiException;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.TreeMap;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import retrofit2.Response;

/**
 * This class obtains info on the currencies communicated via https://blockchain.info/ticker
 */
public class ExchangeRateFactory {

    //Regularly updated ticker data
    private TreeMap<String, TickerItem> tickerData;
    private ExchangeRates api;

    @Inject protected PrefsUtil prefsUtil;

    private static ExchangeRateFactory instance = null;

    private ExchangeRateFactory() {
        Injector.getInstance().getAppComponent().inject(this);
        api = new ExchangeRates(
                BlockchainFramework.getRetrofitExplorerInstance(),
                BlockchainFramework.getApiCode());
    }

    public static ExchangeRateFactory getInstance() {
        if (instance == null) {
            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public Completable updateTicker() {
        return Completable.fromCallable(() -> {
            Response<TreeMap<String, TickerItem>> call = api.getTickerMap().execute();
            if (call.isSuccessful()) {
                tickerData = call.body();
                return Void.TYPE;
            } else {
                throw new ApiException(call.errorBody().string());
            }
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private TickerItem getTickerItem(String currencyName) {
        return tickerData.get(currencyName);
    }

    public double getLastPrice(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = "USD";
        }

        double lastPrice;
        double lastKnown = Double.parseDouble(prefsUtil.getValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currencyName, "0.0"));

        if (tickerData == null) {
            lastPrice = lastKnown;
        } else {
            TickerItem tickerItem = getTickerItem(currencyName);
            lastPrice = tickerItem.getLast();

            if (lastPrice > 0.0) {
                prefsUtil.setValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currencyName, Double.toString(lastPrice));
            } else {
                lastPrice = lastKnown;
            }
        }

        return lastPrice;
    }

    public String getSymbol(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = "USD";
        }

        TickerItem tickerItem = getTickerItem(currencyName);
        return tickerItem.getSymbol();
    }

    /**
     * Returns the historic value of a number of Satoshi at a given time in a given currency. NOTE:
     * Currently only works with USD. May support other currencies in the future.
     *
     * @param satoshis     The amount of Satoshi to be converted
     * @param currency     The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInMillis The time at which to get the price, in milliseconds since epoch
     * @return A double value
     */
    public Observable<Double> getHistoricPrice(long satoshis, String currency, long timeInMillis) {
        return new WalletApi().getHistoricPrice(satoshis, currency, timeInMillis)
                .flatMap(responseBody -> parseStringValue(responseBody.string()))
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Double> parseStringValue(String value) {
        return Observable.fromCallable(() -> {
            // Historic prices are in English format, using Locale.getDefault() will result in
            // a parse exception in some regions
            NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
            Number number = format.parse(value);
            return number.doubleValue();
        });
    }

    public String[] getCurrencyLabels() {
        return tickerData.keySet().toArray(new String[0]);
    }
}
