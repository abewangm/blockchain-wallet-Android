package piuk.blockchain.android.util;

import info.blockchain.api.data.Ticker;
import info.blockchain.api.data.TickerItem;
import info.blockchain.api.exchangerates.ExchangeRates;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.exceptions.ApiException;

import org.apache.commons.lang3.EnumUtils;

import java.text.NumberFormat;
import java.util.Locale;

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
    private Ticker tickerData;
    private ExchangeRates api;

    @Inject protected PrefsUtil prefsUtil;

    private static ExchangeRateFactory instance = null;

    public enum Currency {
        AUD, BRL, CAD, CHF, CLP, CNY, DKK, EUR, GBP, HKD,
        ISK, JPY, KRW, NZD, PLN, RUB, SEK, SGD, THB, TWD, USD
    }

    private ExchangeRateFactory() {
        Injector.getInstance().getAppComponent().inject(this);
        api = new ExchangeRates(
                BlockchainFramework.getRetrofitServerInstance(),
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
            Response<Ticker> call = api.getTicker().execute();
            if (call.isSuccessful()) {
                tickerData = call.body();
                return Void.TYPE;
            } else {
                throw new ApiException(call.errorBody().string());
            }
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private TickerItem getTickerItem(Currency currency) {
        switch (currency) {
            case AUD:
                return tickerData.getAUD();
            case BRL:
                return tickerData.getBRL();
            case CAD:
                return tickerData.getCAD();
            case CHF:
                return tickerData.getCHF();
            case CLP:
                return tickerData.getCLP();
            case CNY:
                return tickerData.getCNY();
            case DKK:
                return tickerData.getDKK();
            case EUR:
                return tickerData.getEUR();
            case GBP:
                return tickerData.getGBP();
            case HKD:
                return tickerData.getHKD();
            case ISK:
                return tickerData.getISK();
            case JPY:
                return tickerData.getJPY();
            case KRW:
                return tickerData.getKRW();
            case NZD:
                return tickerData.getNZD();
            case PLN:
                return tickerData.getPLN();
            case RUB:
                return tickerData.getRUB();
            case SEK:
                return tickerData.getSEK();
            case SGD:
                return tickerData.getSGD();
            case THB:
                return tickerData.getTHB();
            case TWD:
                return tickerData.getTWD();
            case USD:
                return tickerData.getUSD();
            default:
                return tickerData.getUSD();
        }
    }

    public double getLastPrice(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = Currency.USD.name();
        }

        double lastPrice;
        Currency currency = Currency.valueOf(currencyName.toUpperCase().trim());
        double lastKnown = Double.parseDouble(prefsUtil.getValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, "0.0"));

        if (tickerData == null) {
            lastPrice = lastKnown;
        } else {
            TickerItem tickerItem = getTickerItem(currency);
            lastPrice = tickerItem.getLast();

            if (lastPrice > 0.0) {
                prefsUtil.setValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, Double.toString(lastPrice));
            } else {
                lastPrice = lastKnown;
            }
        }

        return lastPrice;
    }

    public String getSymbol(String currencyName) {
        if (currencyName.isEmpty()) {
            currencyName = Currency.USD.name();
        }

        Currency currency = Currency.valueOf(currencyName.toUpperCase().trim());

        TickerItem tickerItem = getTickerItem(currency);
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

    /**
     * Parse the data supplied to this instance.
     */
    public void setData(Ticker data) {
        tickerData = data;
    }

    public String[] getCurrencyLabels() {
        return EnumUtils.getEnumMap(Currency.class).keySet().toArray(new String[0]);
    }
}
