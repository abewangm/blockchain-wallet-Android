package piuk.blockchain.android.data.datamanagers;

import io.reactivex.Observable;
import piuk.blockchain.android.data.access.AccessState;

public class OnboardingDataManager {

    private SettingsDataManager settingsDataManager;
    private AuthDataManager authDataManager;
    private PayloadDataManager payloadDataManager;
    private AccessState accessState;

    public OnboardingDataManager(SettingsDataManager settingsDataManager,
                                 AuthDataManager authDataManager,
                                 PayloadDataManager payloadDataManager,
                                 AccessState accessState) {
        this.settingsDataManager = settingsDataManager;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
        this.accessState = accessState;
    }

    /**
     * Returns whether or not a user is accessing their wallet from a SEPA country, ie should be
     * able to see buy/sell prompts.
     */
    public boolean isSepa() {
        return accessState.getInSepaCountry();
    }

    /**
     * Returns the current Buy/Sell rollout percent for Android. If 0, Buy/Sell should be disabled.
     */
    public double getRolloutPercentage() {
        return accessState.getBuySellRolloutPercent();
    }

    /**
     * Checks whether or not a user is accessing their wallet from a SEPA country and stores the
     * result in {@link AccessState}. Also stores the current rollout value for Android.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    public Observable<Boolean> getIfSepaCountry() {
        return authDataManager.getWalletOptions()
                .flatMap(walletOptions -> settingsDataManager.initSettings(
                        payloadDataManager.getWallet().getGuid(),
                        payloadDataManager.getWallet().getSharedKey())
                        .map(settings -> walletOptions.getBuySellCountries().contains(settings.getCountryCode()))
                        .doOnNext(sepaCountry -> accessState.setInSepaCountry(sepaCountry))
                        .doOnNext(ignored -> accessState.setBuySellRolloutPercent(walletOptions.getRolloutPercentage())));
    }
}
