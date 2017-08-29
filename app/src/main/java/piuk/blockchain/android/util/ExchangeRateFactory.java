package piuk.blockchain.android.util;

import info.blockchain.api.data.TickerItem;
import info.blockchain.api.exchangerates.ExchangeRates;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.exceptions.ApiException;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TreeMap;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import retrofit2.Response;

/**
 * This class obtains info on the currencies communicated via https://blockchain.info/ticker
 */
public class ExchangeRateFactory {

    private static final String PREF_LAST_KNOWN_BTC_PRICE = "LAST_KNOWN_BTC_VALUE_FOR_CURRENCY_";
    private static final String PREF_LAST_KNOWN_ETH_PRICE = "LAST_KNOWN_ETH_VALUE_FOR_CURRENCY_";

    private final WalletApi walletApi = new WalletApi();
    //Regularly updated ticker data
    private TreeMap<String, TickerItem> btcTickerData;
    private TreeMap<String, TickerItem> ethTickerData;
    private ExchangeRates api;

    @Inject protected PrefsUtil prefsUtil;
    @Inject protected RxBus rxBus;
    private RxPinning rxPinning;

    private static ExchangeRateFactory instance = null;

    private ExchangeRateFactory() {
        Injector.getInstance().getAppComponent().inject(this);
        api = new ExchangeRates(
                BlockchainFramework.getRetrofitExplorerInstance(),
                BlockchainFramework.getRetrofitApiInstance(),
                BlockchainFramework.getApiCode());

        rxPinning = new RxPinning(rxBus);
    }

    public static ExchangeRateFactory getInstance() {
        if (instance == null) {
            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public Completable updateTickers() {
        return rxPinning.call(() -> getBtcTicker().mergeWith(getEthTicker()));
    }

    public double getLastBtcPrice(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = "USD";
        }

        double lastPrice;
        double lastKnown = Double.parseDouble(prefsUtil.getValue(PREF_LAST_KNOWN_BTC_PRICE + currencyName, "0.0"));

        if (btcTickerData == null) {
            lastPrice = lastKnown;
        } else {
            TickerItem tickerItem = getBtcTickerItem(currencyName);
            lastPrice = tickerItem.getLast();

            if (lastPrice > 0.0) {
                prefsUtil.setValue(PREF_LAST_KNOWN_BTC_PRICE + currencyName, Double.toString(lastPrice));
            } else {
                lastPrice = lastKnown;
            }
        }

        return lastPrice;
    }

    public double getLastEthPrice(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = "USD";
        }

        double lastPrice;
        double lastKnown = Double.parseDouble(prefsUtil.getValue(PREF_LAST_KNOWN_ETH_PRICE + currencyName, "0.0"));

        if (ethTickerData == null) {
            lastPrice = lastKnown;
        } else {
            TickerItem tickerItem = getEthTickerItem(currencyName);
            lastPrice = tickerItem.getLast();

            if (lastPrice > 0.0) {
                prefsUtil.setValue(PREF_LAST_KNOWN_ETH_PRICE + currencyName, Double.toString(lastPrice));
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

        TickerItem tickerItem = getBtcTickerItem(currencyName);
        return tickerItem.getSymbol();
    }

    /**
     * Returns the historic value of a number of Satoshi at a given time in a given currency.
     *
     * @param satoshis     The amount of Satoshi to be converted
     * @param currency     The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInMillis The time at which to get the price, in milliseconds since epoch
     * @return A double value
     */
    public Observable<Double> getBtcHistoricPrice(long satoshis, String currency, long timeInMillis) {
        return walletApi.getBtcHistoricPrice(satoshis, currency, timeInMillis)
                .flatMap(responseBody -> parseStringValue(responseBody.string()))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns the historic value of a number of Wei at a given time in a given currency.
     *
     * @param wei          The amount of Ether to be converted in Wei, ie ETH * 1e18
     * @param currency     The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInMillis The time at which to get the price, in milliseconds since epoch
     * @return A double value
     */
    public Observable<Double> getEthHistoricPrice(BigInteger wei, String currency, long timeInMillis) {
        return walletApi.getEthHistoricPrice(wei, currency, timeInMillis)
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
        return btcTickerData.keySet().toArray(new String[0]);
    }

    private Completable getBtcTicker() {
        return Completable.fromCallable(() -> {
            Response<TreeMap<String, TickerItem>> call = api.getBtcTickerMap().execute();
            if (call.isSuccessful()) {
                btcTickerData = call.body();
                return Void.TYPE;
            } else {
                throw new ApiException(call.errorBody().string());
            }
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private Completable getEthTicker() {
        return Completable.fromCallable(() -> {
            Response<TreeMap<String, TickerItem>> call = api.getEthTickerMap().execute();
            if (call.isSuccessful()) {
                ethTickerData = call.body();
                return Void.TYPE;
            } else {
                throw new ApiException(call.errorBody().string());
            }
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private TickerItem getBtcTickerItem(String currencyName) {
        return btcTickerData.get(currencyName);
    }

    private TickerItem getEthTickerItem(String currencyName) {
        return ethTickerData.get(currencyName);
    }
}
