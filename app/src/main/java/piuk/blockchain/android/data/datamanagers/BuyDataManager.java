package piuk.blockchain.android.data.datamanagers;

import io.reactivex.Observable;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {
    private OnboardingDataManager onboardingDataManager;
    private SettingsDataManager settingsDataManager;
    private PayloadDataManager payloadDataManager;

    public BuyDataManager(OnboardingDataManager onboardingDataManager, SettingsDataManager settingsDataManager, PayloadDataManager payloadDataManager) {
        this.onboardingDataManager = onboardingDataManager;
        this.settingsDataManager = settingsDataManager;
        this.payloadDataManager = payloadDataManager;
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
}
