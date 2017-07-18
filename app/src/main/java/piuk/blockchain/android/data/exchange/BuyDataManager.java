package piuk.blockchain.android.data.exchange;

import org.bitcoinj.core.Sha256Hash;
import org.spongycastle.util.encoders.Hex;

import io.reactivex.Observable;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.data.settings.SettingsDataManager;

/**
 * Created by justin on 4/28/17.
 */

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
     * Checks whether or not a user is accessing their wallet from a SEPA country and stores the
     * result in {@link AccessState}. Also stores the current rollout value for Android.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    Observable<Boolean> getIfSepaCountry() {
        return authDataManager.getWalletOptions()
                .flatMap(walletOptions -> settingsDataManager.getSettings()
                        .map(settings -> walletOptions.getBuySellCountries().contains(settings.getCountryCode()))
                        .doOnNext(sepaCountry -> accessState.setInSepaCountry(sepaCountry))
                        .doOnNext(ignored -> accessState.setBuySellRolloutPercent(walletOptions.getRolloutPercentage())));
    }

    /**
     * Checks whether or not buy/sell is allowed to be rolled out based on percentage check on
     * user's GUID.
     *
     * @return An {@link Observable} wrapping a boolean value
     */
    boolean isRolloutAllowed() {
        String plainGuid = payloadDataManager.getWallet().getGuid().replace("-", "");

        byte[] guidHashBytes = Sha256Hash.hash(Hex.encode(plainGuid.getBytes()));
        int unsignedByte = guidHashBytes[0] & 0xff;
        double rolloutPercentage = accessState.getBuySellRolloutPercent();

        return ((unsignedByte + 1.0) / 256.0) <= rolloutPercentage;
    }

    public Observable<WebViewLoginDetails> getWebViewLoginDetails() {
        return exchangeService.getWebViewLoginDetails();
    }

    public Observable<String> watchPendingTrades() {
        return exchangeService.watchPendingTrades();
    }

    public synchronized Observable<Boolean> getCanBuy() {

        return Observable.combineLatest(getIfSepaCountry(), exchangeService.hasCoinifyAccount(),
                (isSepa, hasAccount) -> hasAccount || isSepa && isRolloutAllowed());
    }

    public void reloadExchangeData() {
        exchangeService.reloadExchangeData();
    }

    public void wipe() {
        exchangeService.wipe();
    }

}
