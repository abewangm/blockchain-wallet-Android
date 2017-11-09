package piuk.blockchain.android.data.walletoptions;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.subjects.ReplaySubject;

public class WalletOptionsState {

    private static WalletOptionsState instance;

    ReplaySubject<WalletOptions> walletOptionsSource;
    ReplaySubject<Settings> walletSettingsSource;

    private boolean replayProtectionActive = false;

    private WalletOptionsState(ReplaySubject<WalletOptions> walletOptionsSource,
                                     ReplaySubject<Settings> walletSettingsSource) {
        this.walletOptionsSource = walletOptionsSource;
        this.walletSettingsSource = walletSettingsSource;
    }

    public static WalletOptionsState getInstance(ReplaySubject<WalletOptions> walletOptionsSubject,
                                                       ReplaySubject<Settings> walletSettingsSubject) {
        if (instance == null)
            instance = new WalletOptionsState(walletOptionsSubject, walletSettingsSubject);
        return instance;
    }

    public void setReplayProtectionActive(boolean replayProtectionActive) {
        this.replayProtectionActive = replayProtectionActive;
    }

    public boolean isReplayProtectionActive() {
        return replayProtectionActive;
    }

    public void destroy() {
        instance = null;
    }
}
