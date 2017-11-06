package piuk.blockchain.android.data.walletoptions;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.WalletOptions;
import io.reactivex.subjects.ReplaySubject;

public class WalletOptionsState {

    private static WalletOptionsState instance;

    ReplaySubject<WalletOptions> walletOptionsSource;
    ReplaySubject<Settings> walletSettingsSource;

    boolean replayProtectionStatus = false;

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

    public void setReplayProtectionStatus(boolean replayProtectionStatus) {
        this.replayProtectionStatus = replayProtectionStatus;
    }

    public boolean isReplayProtectionStatus() {
        return replayProtectionStatus;
    }

    public void destroy() {
        instance = null;
    }
}
