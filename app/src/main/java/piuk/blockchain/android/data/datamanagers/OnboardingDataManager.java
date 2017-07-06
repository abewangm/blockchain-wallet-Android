package piuk.blockchain.android.data.datamanagers;

import android.util.Log;

import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;

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
                        .map(settings -> {

                            boolean isInCoinifyCountry = walletOptions.getPartners()
                                    .getCoinify().getCountries().contains(settings.getCountryCode());
                            accessState.setInSepaCountry(isInCoinifyCountry);
                            return isInCoinifyCountry;
                        })
                        .doOnNext(ignored -> {
                            accessState.setBuySellRolloutPercent(walletOptions.getRolloutPercentage());
                        }));
    }

    /**
     * Checks whether or not buy/sell is allowed to be rolled out based on percentage check on user's GUID.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    public Observable<Boolean> isRolloutAllowed() {

        String plainGuid = payloadDataManager.getWallet().getGuid().replace("-", "");

        byte[] guidHashBytes = Sha256Hash.hash(Hex.encode(plainGuid.getBytes()));
        int unsignedByte = guidHashBytes[0] & 0xff;
        double rolloutPercentage = accessState.getBuySellRolloutPercent();

        boolean userHasAccess = ((unsignedByte + 1.0) / 256.0) <= rolloutPercentage;

        return Observable.just(userHasAccess);
    }
}
