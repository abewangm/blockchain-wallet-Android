package piuk.blockchain.android.data.exchange;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;

import io.reactivex.subjects.ReplaySubject;

public class BuyConditions {

    private static BuyConditions instance;

    ReplaySubject<WalletOptions> walletOptionsSource;
    ReplaySubject<Settings> walletSettingsSource;
    ReplaySubject<Boolean> coinifyWhitelistedSource;//Only coinify whitelisted like this. Other partners whitelisted in wallet settings

    private BuyConditions(ReplaySubject<WalletOptions> walletOptionsSource,
                          ReplaySubject<Settings> walletSettingsSource,
                          ReplaySubject<Boolean> coinifyWhitelistedSource) {
        this.walletOptionsSource = walletOptionsSource;
        this.walletSettingsSource = walletSettingsSource;
        this.coinifyWhitelistedSource = coinifyWhitelistedSource;
    }

    public static BuyConditions getInstance(ReplaySubject<WalletOptions> walletOptionsSubject,
                                            ReplaySubject<Settings> walletSettingsSubject,
                                            ReplaySubject<Boolean> coinifyWhitelistedSubject) {
        if (instance == null)
            instance = new BuyConditions(walletOptionsSubject, walletSettingsSubject, coinifyWhitelistedSubject);
        return instance;
    }

}
