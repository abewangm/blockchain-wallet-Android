package piuk.blockchain.android.data.walletoptions;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.subjects.ReplaySubject;

public class WalletOptionsState {

    private static WalletOptionsState instance;

    ReplaySubject<WalletOptions> walletOptionsSource;
    ReplaySubject<Settings> walletSettingsSource;

    private boolean americanStateSelectionRequired = false;
    private String americanState;

    private WalletOptionsState(ReplaySubject<WalletOptions> walletOptionsSource,
                                     ReplaySubject<Settings> walletSettingsSource) {
        this.walletOptionsSource = walletOptionsSource;
        this.walletSettingsSource = walletSettingsSource;
        americanState = null;
    }

    public static WalletOptionsState getInstance(ReplaySubject<WalletOptions> walletOptionsSubject,
                                                       ReplaySubject<Settings> walletSettingsSubject) {
        if (instance == null)
            instance = new WalletOptionsState(walletOptionsSubject, walletSettingsSubject);
        return instance;
    }

    public void destroy() {
        instance = null;
    }

    public boolean isAmericanStateSelectionRequired() {
        return americanStateSelectionRequired;
    }

    public void setAmericanStateSelectionRequired(boolean required) {
        americanStateSelectionRequired = required;
    }

    public String getAmericanState() {
        return americanState;
    }

    public void setAmericanState(String americanState) {
        this.americanState = americanState;
    }
}
