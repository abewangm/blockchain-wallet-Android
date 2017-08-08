package piuk.blockchain.android.data.exchange;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.subjects.ReplaySubject;

public class BuyConditions {

    private static BuyConditions instance;

    public ReplaySubject<WalletOptions> walletOptionsSubject;
    public ReplaySubject<Settings> walletSettingsSubject;
    public ReplaySubject<Boolean> coinifyWhitelistedSubject;

    private BuyConditions(ReplaySubject<WalletOptions> walletOptionsSubject, ReplaySubject<Settings> walletSettingsSubject, ReplaySubject<Boolean> coinifyWhitelistedSubject) {
        this.walletOptionsSubject = walletOptionsSubject;
        this.walletSettingsSubject = walletSettingsSubject;
        this.coinifyWhitelistedSubject = coinifyWhitelistedSubject;
    }

    public static BuyConditions getInstance(ReplaySubject<WalletOptions> walletOptionsSubject,
                                            ReplaySubject<Settings> walletSettingsSubject,
                                            ReplaySubject<Boolean> coinifyWhitelistedSubject) {
        if (instance == null)
            instance = new BuyConditions(walletOptionsSubject, walletSettingsSubject, coinifyWhitelistedSubject);
        return instance;
    }
}
