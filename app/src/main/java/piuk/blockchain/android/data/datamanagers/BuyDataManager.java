package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.metadata.Metadata;
import io.reactivex.Observable;
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

    public BuyDataManager(OnboardingDataManager onboardingDataManager, SettingsDataManager settingsDataManager, PayloadDataManager payloadDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;
        this.exchangeService = ExchangeService.getInstance();
    }

    public Observable<Metadata> getExchangeData() {
        return this.exchangeService.getExchangeData();
    }

    public Observable<String> getPendingTradeAddresses() {
        return exchangeService.getPendingTradeAddresses();
    }

    public Observable<Boolean> getCanBuy() {
        return Observable.combineLatest(
                this.onboardingDataManager.getIfSepaCountry(),
                this.getIsInvited(),
                (isSepa, isInvited) -> isSepa && isInvited
        );
    }

    private Observable<Boolean> getIsInvited() {
        return this.settingsDataManager.initSettings(
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getWallet().getSharedKey()
        ).map(settings -> {
            // TODO: implement settings.invited.sfox
            return true;
        });
    }

    public void reloadExchangeData() {
        this.exchangeService.reloadExchangeData();
    }
}
