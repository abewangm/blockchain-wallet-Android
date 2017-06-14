package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.Environment;

import io.reactivex.Observable;
import piuk.blockchain.android.data.api.EnvironmentSettings;
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
    private EnvironmentSettings environmentSettings;

    public BuyDataManager(OnboardingDataManager onboardingDataManager,
                          SettingsDataManager settingsDataManager,
                          PayloadDataManager payloadDataManager,
                          EnvironmentSettings environmentSettings) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;
        exchangeService = ExchangeService.getInstance();
        this.environmentSettings = environmentSettings;
    }

    public Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return exchangeService.getWebViewLoginDetails();
    }

    public Observable<String> watchPendingTrades() {
        return exchangeService.watchPendingTrades();
    }

    public synchronized Observable<Boolean> getCanBuy() {
        if (!environmentSettings.getEnvironment().equals(Environment.PRODUCTION)) {
            return Observable.just(true);
        } else if (payloadDataManager.isDoubleEncrypted()) {
            // TODO: 14/06/2017 In the future, use the Metadata node to restore the master seed
            return Observable.just(false);
        } else {
            return Observable.combineLatest(
                    onboardingDataManager.isRolloutAllowed(),
                    onboardingDataManager.getIfSepaCountry(),
                    getIsInvited(),
                    (allowRollout, isSepa, isInvited) -> allowRollout && (isSepa || isInvited));
        }
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
