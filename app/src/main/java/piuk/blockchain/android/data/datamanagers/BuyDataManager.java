package piuk.blockchain.android.data.datamanagers;

import io.reactivex.Observable;

/**
 * Created by justin on 4/28/17.
 */

public class BuyDataManager {
    private OnboardingDataManager onboardingDataManager;

    public BuyDataManager(OnboardingDataManager onboardingDataManager) {
        this.onboardingDataManager = onboardingDataManager;
    }

    public Observable<Boolean> getCanBuy() {
        return this.onboardingDataManager.getIfSepaCountry();
    }
}
