package piuk.blockchain.android.util;


import android.util.Log;

import info.blockchain.api.data.Ticker;
import info.blockchain.api.data.TickerItem;
import info.blockchain.api.exchangerates.ExchangeRates;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.WalletApi;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.NotImplementedException;

import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;

import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.Response;

/**
 * This class obtains info on the currencies communicated via https://blockchain.info/ticker
 */
// TODO: 22/02/2017 tests
public class ExchangeRateFactory {

    private String TAG = getClass().getName();

    private ExchangeRates api;
    private Timer timer;

    //Regularly updated ticker data
    private Ticker tickerData;
    private final int UPDATE_INTERVAL_MIN = 5;

    @Inject protected PrefsUtil mPrefsUtil;

    private static ExchangeRateFactory instance = null;

    public enum Currency {
        AUD, BRL, CAD, CHF, CLP, CNY, DKK, EUR, GBP, HKD,
        ISK, JPY, KRW, NZD, PLN, RUB, SEK, SGD, THB, TWD, USD
    }

    private ExchangeRateFactory() {
        Injector.getInstance().getAppComponent().inject(this);
        try {
            api = new ExchangeRates(
                BlockchainFramework.getRetrofitServerInstance(),
                BlockchainFramework.getApiCode());

        } catch (IOException e) {
            Log.e(TAG, "ExchangeRateFactory: ", e);
        }
    }

    public static ExchangeRateFactory getInstance() {
        if (instance == null) {
            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public interface TickerListener {
        void onTickerUpdate();
    }

    public void startTicker(TickerListener listener) {
        timer = new Timer();
        timer.scheduleAtFixedRate(tickerTask(listener), 0, 60000 * UPDATE_INTERVAL_MIN);
    }

    public void stopTicker() {
        if(timer != null) {
            timer.cancel();
        }
    }

    private TimerTask tickerTask(TickerListener listener) {

        return new TimerTask() {
            @Override
            public void run() {
                try {
                    Response<Ticker> execute = api.getTicker().execute();

                    if(execute.isSuccessful()) {

                        Log.d(TAG, "Refreshing exchange rate");
                        tickerData = execute.body();
                        listener.onTickerUpdate();

                    } else {
                        Log.e(TAG, "Failed to refresh ticker");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private TickerItem getTickerItem(Currency currency) {

        TickerItem item;

        switch (currency) {
            case AUD: item = tickerData.getAUD(); break;
            case BRL: item = tickerData.getBRL(); break;
            case CAD: item = tickerData.getCAD(); break;
            case CHF: item = tickerData.getCHF(); break;
            case CLP: item = tickerData.getCLP(); break;
            case CNY: item = tickerData.getCNY(); break;
            case DKK: item = tickerData.getDKK(); break;
            case EUR: item = tickerData.getEUR(); break;
            case GBP: item = tickerData.getGBP(); break;
            case HKD: item = tickerData.getHKD(); break;
            case ISK: item = tickerData.getISK(); break;
            case JPY: item = tickerData.getJPY(); break;
            case KRW: item = tickerData.getKRW(); break;
            case NZD: item = tickerData.getNZD(); break;
            case PLN: item = tickerData.getPLN(); break;
            case RUB: item = tickerData.getRUB(); break;
            case SEK: item = tickerData.getSEK(); break;
            case SGD: item = tickerData.getSGD(); break;
            case THB: item = tickerData.getTHB(); break;
            case TWD: item = tickerData.getTWD(); break;
            case USD: item = tickerData.getUSD(); break;
            default: item = tickerData.getUSD();
        }

        return item;
    }

    public double getLastPrice(String currencyName) {

        if(currencyName.isEmpty()) {
            currencyName = Currency.USD.name();
        }

        double lastPrice;
        Currency currency = Currency.valueOf(currencyName.toUpperCase().trim());
        double lastKnown = Double.parseDouble(mPrefsUtil.getValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency, "0.0"));

        if(tickerData == null) {
            lastPrice = lastKnown;
        } else {
            TickerItem tickerItem = getTickerItem(currency);

            lastPrice = tickerItem.getLast();

            if (lastPrice > 0.0) {
                mPrefsUtil.setValue("LAST_KNOWN_VALUE_FOR_CURRENCY_" + currency.toString(), Double.toString(lastPrice));
            } else {
                lastPrice = lastKnown;
            }
        }

        return lastPrice;
    }

    public String getSymbol(String currencyName) {

        if(currencyName.isEmpty()) {
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
    public Observable<Double> getHistoricPrice(long satoshis, String currency, long timeInMillis)
        throws Exception {

        Response<ResponseBody> execute = WalletApi
            .getHistoricPrice(satoshis, currency, timeInMillis).execute();

        if(execute.isSuccessful()) {
            return Observable.just(execute.body().string())
                .flatMap(this::parseStringValue)
                .compose(RxUtil.applySchedulersToObservable());
        } else {
            throw new Exception("Error fetching historic price.");
        }
    }

    private Observable<Double> parseStringValue(String value) {
        return Observable.fromCallable(() -> {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            Number number = format.parse(value);
            return number.doubleValue();
        });
    }

    /**
     * Parse the data supplied to this instance.
     */
    public void setData(Ticker data) {
        this.tickerData = data;
    }
}
