package piuk.blockchain.android.ui.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.data.notifications.FcmCallbackService.EXTRA_CONTACT_ACCEPTED;


@SuppressWarnings("WeakerAccess")
public class LauncherViewModel extends BaseViewModel {

    public static final String INTENT_EXTRA_VERIFIED = "verified";

    @Inject protected AppUtil appUtil;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AccessState accessState;
    @Inject protected SettingsDataManager settingsDataManager;
    private DataListener dataListener;

    public interface DataListener {

        Intent getPageIntent();

        void onNoGuid();

        void onRequestPin();

        void onCorruptPayload();

        void onRequestUpgrade();

        void onStartMainActivity();

        void onReEnterPassword();

        void onStartOnboarding(boolean emailOnly);

    }

    public LauncherViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        dataListener = listener;
    }

    @Override
    public void onViewReady() {
        // Store incoming URI if needed
        Intent intent = dataListener.getPageIntent();
        String action = intent.getAction();
        String scheme = intent.getScheme();
        String intentData = intent.getDataString();
        if (action != null && Intent.ACTION_VIEW.equals(action) && scheme != null && scheme.equals("bitcoin")) {
            prefsUtil.setValue(PrefsUtil.KEY_SCHEME_URL, intent.getData().toString());
        }

        if (action != null && Intent.ACTION_VIEW.equals(action) && intentData != null && intentData.contains("blockchain")) {
            prefsUtil.setValue(PrefsUtil.KEY_METADATA_URI, intentData);
        }

        if (intent.hasExtra(EXTRA_CONTACT_ACCEPTED)) {
            prefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true);
        }

        boolean isPinValidated = false;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED);
        }

        boolean hasLoggedOut = prefsUtil.getValue(PrefsUtil.LOGGED_OUT, false);

        if (prefsUtil.getValue(PrefsUtil.KEY_GUID, "").isEmpty()) {
            // No GUID? Treat as new installation
            dataListener.onNoGuid();

        } else if (hasLoggedOut) {
            // User has logged out recently. Show password reentry page
            dataListener.onReEnterPassword();

        } else if (prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            // No PIN ID? Treat as installed app without confirmed PIN
            dataListener.onRequestPin();

        } else if (!appUtil.isSane()) {
            // Installed app, check sanity
            dataListener.onCorruptPayload();

        } else if (isPinValidated && !payloadDataManager.getWallet().isUpgraded()) {
            // Legacy app has not been prompted for upgrade
            accessState.setIsLoggedIn(true);
            dataListener.onRequestUpgrade();

        } else if (isPinValidated || (accessState.isLoggedIn())) {
            // App has been PIN validated
            accessState.setIsLoggedIn(true);
            if (appUtil.isNewlyCreated()) {
                dataListener.onStartOnboarding(false);
            } else {
                compositeDisposable.add(
                        settingsDataManager.initSettings(
                                payloadDataManager.getWallet().getGuid(),
                                payloadDataManager.getWallet().getSharedKey())
                                .subscribe(settings -> {
                                    if (!settings.isEmailVerified()
                                            && settings.getEmail() != null
                                            && !settings.getEmail().isEmpty()) {
                                        int visits = prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0);
                                        if (visits == 1) {
                                            // Nag user to verify email after second login
                                            dataListener.onStartOnboarding(true);
                                        } else {
                                            dataListener.onStartMainActivity();
                                        }

                                        visits++;
                                        prefsUtil.setValue(PrefsUtil.KEY_APP_VISITS, visits);
                                    } else {
                                        dataListener.onStartMainActivity();
                                    }
                                }, throwable -> dataListener.onStartMainActivity()));

            }
        } else {
            dataListener.onRequestPin();
        }
    }

    @NonNull
    public AppUtil getAppUtil() {
        return appUtil;
    }

}
