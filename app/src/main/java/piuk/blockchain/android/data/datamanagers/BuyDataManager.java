package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.api.Environment;

import info.blockchain.wallet.metadata.MetadataNodeFactory;
import io.reactivex.Observable;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.data.services.ExchangeService;
import piuk.blockchain.android.data.settings.SettingsDataManager;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {

    private OnboardingDataManager onboardingDataManager;
    private SettingsDataManager settingsDataManager;
    private ExchangeService exchangeService;
    private EnvironmentSettings environmentSettings;

    public BuyDataManager(OnboardingDataManager onboardingDataManager,
                          SettingsDataManager settingsDataManager,
                          EnvironmentSettings environmentSettings) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.exchangeService = ExchangeService.getInstance();
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
        } else {
            return Observable.combineLatest(
                    onboardingDataManager.isRolloutAllowed(),
                    onboardingDataManager.getIfSepaCountry(),
                    getIsInvited(),
                    (allowRollout, isSepa, isInvited) -> allowRollout && (isSepa || isInvited));
        }
    }

    private Observable<Boolean> getIsInvited() {
        return settingsDataManager.getSettings()
                .map(settings -> {
                    // TODO: implement settings.invited.sfox
                    return false;
                });
    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }
}
