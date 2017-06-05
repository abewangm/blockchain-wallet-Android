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

    public Observable<Boolean> getCanBuy() {
        return Observable.combineLatest(
                onboardingDataManager.getIfSepaCountry(),
                getIsInvited(),
                (isSepa, isInvited) -> isSepa || isInvited);
    }

    private Observable<Boolean> getIsInvited() {
        return settingsDataManager.initSettings(
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getWallet().getSharedKey())
                .map(settings -> {
                    // TODO: implement settings.invited.sfox
                    return false;
                });
    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }
}
