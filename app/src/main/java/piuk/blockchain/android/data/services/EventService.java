package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.WalletApi;

import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.util.PrefsUtil;


public class EventService {

    public static final String EVENT_2ND_PW = "wallet_login_second_password_";
    public static final String EVENT_LEGACY = "wallet_login_legacy_use_";
    public static final String EVENT_BACKUP = "wallet_login_android_recovery_phrase_";

    public static final String EVENT_TX_INPUT_FROM_QR = "wallet_android_tx_from_qr";
    public static final String EVENT_TX_INPUT_FROM_PASTE = "wallet_android_tx_from_paste";
    public static final String EVENT_TX_INPUT_FROM_URI = "wallet_android_tx_from_uri";
    public static final String EVENT_TX_INPUT_FROM_DROPDOWN = "wallet_android_tx_from_dropdown";
    public static final String EVENT_TX_INPUT_FROM_CONTACTS = "wallet_android_tx_from_contacts";

    private PrefsUtil prefsUtil;
    private WalletApi walletApi;

    public EventService(PrefsUtil prefsUtil, WalletApi walletApi) {
        this.walletApi = walletApi;
        this.prefsUtil = prefsUtil;
    }

    public void log2ndPwEvent(boolean active) {
        logEventOnce(PrefsUtil.KEY_EVENT_2ND_PW, EVENT_2ND_PW + getBoolean(active));
    }

    public void logLegacyEvent(boolean active) {
        // Disabled until further notice
//        logEventOnce(PrefsUtil.KEY_EVENT_LEGACY, EVENT_LEGACY + getBoolean(active));
    }

    public void logBackupEvent(boolean active) {
        // Disabled until further notice
//        logEventOnce(PrefsUtil.KEY_EVENT_BACKUP, EVENT_BACKUP + getBoolean(active));
    }

    public void logAddressInputEvent(String flag) {
        logEvent(flag);
    }

    private String getBoolean(boolean active) {
        return active ? "1" : "0";
    }

    private void logEvent(String eventName) {

        walletApi.logEvent(eventName)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    // No-op
                }, Throwable::printStackTrace);
    }

    private void logEventOnce(String key, String eventName) {
        if (!prefsUtil.getValue(key, false)) {
            walletApi.logEvent(eventName)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            response -> prefsUtil.setValue(key, true),
                            Throwable::printStackTrace);
        }
    }
}
