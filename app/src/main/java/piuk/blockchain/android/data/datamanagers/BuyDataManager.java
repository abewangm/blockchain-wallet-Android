package piuk.blockchain.android.data.datamanagers;

import io.reactivex.Observable;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.data.services.ExchangeService;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {
    public static final String TAG = BuyDataManager.class.getSimpleName();

    private OnboardingDataManager onboardingDataManager;
    private SettingsDataManager settingsDataManager;
    private PayloadDataManager payloadDataManager;
    private ExchangeService exchangeService;

    public BuyDataManager(OnboardingDataManager onboardingDataManager,
                          SettingsDataManager settingsDataManager,
                          PayloadDataManager payloadDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;
        exchangeService = ExchangeService.getInstance();
    }

    public Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return exchangeService.getWebViewLoginDetails();
    }

    public Observable<String> watchPendingTrades() {
        return exchangeService.watchPendingTrades();
    }

    public synchronized Observable<Boolean> getCanBuy() {
        if (payloadDataManager.isDoubleEncrypted()) {
            // TODO: 14/06/2017 In the future, use the Metadata node to restore the master seed
            return Observable.just(false);
        } else {

            return onboardingDataManager.getIfSepaCountry()
                    .flatMap(isSepa -> {
                        if(isSepa) {
                            return onboardingDataManager.isRolloutAllowed();
                        } else {
                            return Observable.just(false);
                        }
                    });
        }
    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }
}
