package piuk.blockchain.android.data.exchange;

import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.Observable;
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
    private BuyConditions buyConditions;

    public BuyDataManager(SettingsDataManager settingsDataManager,
                          AuthDataManager authDataManager,
                          PayloadDataManager payloadDataManager,
                          BuyConditions buyConditions,
                          ExchangeService exchangeService) {
        this.settingsDataManager = settingsDataManager;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
        this.buyConditions = buyConditions;
        this.exchangeService = exchangeService;
    }

    /**
     * ReplaySubjects will re-emit items it observed.
     * It is safe to assumed that walletOptions and
     * the user's country code won't change during an active session.
     */
    private void initReplaySubjects() {
        Observable<WalletOptions> walletOptionsStream = authDataManager.getWalletOptions();
        walletOptionsStream.subscribeWith(buyConditions.walletOptionsSource);

        Observable<Settings> walletSettingsStream = settingsDataManager.getSettings();
        walletSettingsStream.subscribeWith(buyConditions.walletSettingsSource);

        Observable<Boolean> coinifyWhitelistedStream = exchangeService.hasCoinifyAccount();
        coinifyWhitelistedStream.subscribeWith(buyConditions.coinifyWhitelistedSource);
    }

    public synchronized Observable<Boolean> getCanBuy() {

        initReplaySubjects();

        return Observable.combineLatest(isBuyRolledOut(), isCoinifyAllowed(), isUnocoinAllowed(),
                (isBuyRolledOut, allowCoinify, allowUnocoin) -> isBuyRolledOut && (allowCoinify || allowUnocoin));
    }

    /**
     * Checks if buy is rolled out for user on android based on GUID. (All exchange partners)
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    Observable<Boolean> isBuyRolledOut() {

        return buyConditions.walletOptionsSource
                .flatMap(walletOptions -> buyConditions.walletSettingsSource
                        .map(inCoinifyCountry -> isRolloutAllowed(walletOptions.getRolloutPercentage()))
                );
    }

    /**
     * Checks if user has whitelisted coinify account or in valid coinify country
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    public Observable<Boolean> isCoinifyAllowed() {

        return Observable.combineLatest(isInCoinifyCountry(), buyConditions.coinifyWhitelistedSource,
                (coinifyCountry, whiteListed) -> coinifyCountry || whiteListed);
    }

    /**
     * Checks whether or not a user is accessing their wallet from a SEPA country.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    Observable<Boolean> isInCoinifyCountry() {

        return buyConditions.walletOptionsSource
                .flatMap(walletOptions -> buyConditions.walletSettingsSource
                        .map(settings -> walletOptions.getPartners().getCoinify().getCountries().contains(settings.getCountryCode()))
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
    Observable<Boolean> isInUnocoinCountry() {
        return buyConditions.walletOptionsSource
                .flatMap(walletOptions -> buyConditions.walletSettingsSource
                        .map(settings -> walletOptions.getPartners().getUnocoin().getCountries().contains(settings.getCountryCode()))
                );
    }

    public Observable<Boolean> isUnocoinAllowed() {
        return Observable.combineLatest(isInUnocoinCountry(), isUnocoinWhitelisted(),
                (unocoinCountry, whiteListed) -> unocoinCountry || whiteListed);
    }

    Observable<Boolean> isUnocoinWhitelisted() {
        return settingsDataManager.getSettings()
                .map(settings -> settings.getInvited().get("unocoin"));
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
