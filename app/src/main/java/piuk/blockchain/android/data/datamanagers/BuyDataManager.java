package piuk.blockchain.android.data.datamanagers;

import io.reactivex.Observable;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.data.services.ExchangeService;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {

    private OnboardingDataManager onboardingDataManager;
    private ExchangeService exchangeService;

    public BuyDataManager(OnboardingDataManager onboardingDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        exchangeService = ExchangeService.getInstance();
    }

    public Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return exchangeService.getWebViewLoginDetails();
    }

    public Observable<String> watchPendingTrades() {
        return exchangeService.watchPendingTrades();
    }

    public synchronized Observable<Boolean> getCanBuy() {
        return onboardingDataManager.getIfSepaCountry()
                .flatMap(isSepa -> {
                    if (isSepa) {
                        return onboardingDataManager.isRolloutAllowed();
                    } else {
                        return Observable.just(false);
                    }
                });

    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }
}
