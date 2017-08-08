package piuk.blockchain.android.data.exchange;

import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.Observable;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import timber.log.Timber;

public class BuyDataManager {

    private ExchangeService exchangeService;
    private SettingsDataManager settingsDataManager;
    private AuthDataManager authDataManager;
    private PayloadDataManager payloadDataManager;
    private AccessState accessState;

    public BuyDataManager(SettingsDataManager settingsDataManager,
                          AuthDataManager authDataManager,
                          PayloadDataManager payloadDataManager,
                          AccessState accessState,
                          ExchangeService exchangeService) {
        this.settingsDataManager = settingsDataManager;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
        this.accessState = accessState;
        this.exchangeService = exchangeService;
    }

    /**
     * ReplaySubjects will re-emit items it observed.
     * It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    private void initReplaySubjects() {
        Observable<WalletOptions> walletOptionsStream = authDataManager.getWalletOptions();
        walletOptionsStream.subscribeWith(accessState.walletOptionsSubject);

        Observable<Settings> walletSettingsStream = settingsDataManager.getSettings();
        walletSettingsStream.subscribeWith(accessState.walletSettingsSubject);

        Observable<Boolean> coinifyWhitelistedStream = exchangeService.hasCoinifyAccount();
        coinifyWhitelistedStream.subscribeWith(accessState.coinifyWhitelistedSubject);
    }

    public synchronized Observable<Boolean> getCanBuy() {

        initReplaySubjects();

        return Observable.combineLatest(isCoinifyAllowed(), isUnocoinAllowed(),
                (allowCoinify, allowUnocoin) -> allowCoinify || allowUnocoin);
    }

    public synchronized Observable<Boolean> isCoinifyAllowed() {

        return Observable.combineLatest(isCoinifyRolledOut(), accessState.coinifyWhitelistedSubject,
                (coinifyRolledOut, whiteListed) -> coinifyRolledOut || whiteListed);
    }

    /**
     * Checks whether or not a user is accessing their wallet from a SEPA country.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    Observable<Boolean> isCoinifyRolledOut() {

        return accessState.walletOptionsSubject
                .flatMap(walletOptions -> accessState.walletSettingsSubject
                        .map(settings -> walletOptions.getPartners().getCoinify().getCountries().contains(settings.getCountryCode()))
                        .map(inCoinifyCountry -> inCoinifyCountry && isRolloutAllowed(walletOptions.getRolloutPercentage()))
                );
    }

    /**
     * Checks whether or not buy/sell is allowed to be rolled out based on percentage check on
     * user's GUID.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    boolean isRolloutAllowed(double rolloutPercentage) {
        String plainGuid = payloadDataManager.getWallet().getGuid().replace("-", "");

        byte[] guidHashBytes = Sha256Hash.hash(Hex.encode(plainGuid.getBytes()));
        int unsignedByte = guidHashBytes[0] & 0xff;

        return ((unsignedByte + 1.0) / 256.0) <= rolloutPercentage;
    }

    /**
     * Checks whether or not a user is accessing their wallet from India.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    Observable<Boolean> isUnocoinRolledOut() {

        return accessState.walletOptionsSubject
                .flatMap(walletOptions -> accessState.walletSettingsSubject
                        .map(settings -> walletOptions.getPartners().getUnocoin().getCountries().contains(settings.getCountryCode()))
                        .map(inUnocoinCountry -> inUnocoinCountry && isRolloutAllowed(walletOptions.getRolloutPercentage()))
                );
    }

    public synchronized Observable<Boolean> isUnocoinAllowed() {

        // TODO: 08/08/2017 Unocoin is still under development and not ready to release
        return Observable.just(false);

        // TODO: 04/08/2017 Potentially unocoin will have whitelisted accounts
//        return Observable.combineLatest(isUnocoinRolledOut(), accessState.unocoinWhitelistedSubject,
//                (unocoinRolledOut, whiteListed) -> unocoinRolledOut || whiteListed);
    }

    public Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return exchangeService.getWebViewLoginDetails();
    }

    public Observable<String> watchPendingTrades() {
        return exchangeService.watchPendingTrades();
    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }

    public void wipe() {
        exchangeService.wipe();
    }

}
