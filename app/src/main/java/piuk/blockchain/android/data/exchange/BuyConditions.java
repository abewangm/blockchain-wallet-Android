package piuk.blockchain.android.data.exchange;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;

import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.exchange.models.ExchangeData;

public class BuyConditions {

    private static BuyConditions instance;

    ReplaySubject<WalletOptions> walletOptionsSource;
    ReplaySubject<Settings> walletSettingsSource;
    ReplaySubject<ExchangeData> exchangeDataSource;

    private BuyConditions(ReplaySubject<WalletOptions> walletOptionsSource,
                          ReplaySubject<Settings> walletSettingsSource,
                          ReplaySubject<ExchangeData> exchangeDataSource) {
        this.walletOptionsSource = walletOptionsSource;
        this.walletSettingsSource = walletSettingsSource;
        this.exchangeDataSource = exchangeDataSource;
    }

    public static BuyConditions getInstance(ReplaySubject<WalletOptions> walletOptionsSubject,
                                            ReplaySubject<Settings> walletSettingsSubject,
                                            ReplaySubject<ExchangeData> coinifyWhitelistedSubject) {
        if (instance == null)
            instance = new BuyConditions(walletOptionsSubject, walletSettingsSubject, coinifyWhitelistedSubject);
        return instance;
    }

}
