package piuk.blockchain.android.ui.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.data.notifications.FcmCallbackService.EXTRA_CONTACT_ACCEPTED;


@SuppressWarnings("WeakerAccess")
public class LauncherViewModel extends BaseViewModel {

    public static final String INTENT_EXTRA_VERIFIED = "verified";

    @Inject protected AppUtil mAppUtil;
    @Inject protected PayloadDataManager mPayloadDataManager;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected AccessState mAccessState;
    private DataListener mDataListener;

    public interface DataListener {

        Intent getPageIntent();

        void onNoGuid();

        void onRequestPin();

        void onCorruptPayload();

        void onRequestUpgrade();

        void onStartMainActivity();

        void onReEnterPassword();

        void onStartOnboarding();

    }

    public LauncherViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        mDataListener = listener;
    }

    @Override
    public void onViewReady() {
        // Store incoming URI if needed
        Intent intent = mDataListener.getPageIntent();
        String action = intent.getAction();
        String scheme = intent.getScheme();
        String intentData = intent.getDataString();
        if (action != null && Intent.ACTION_VIEW.equals(action) && scheme != null && scheme.equals("bitcoin")) {
            mPrefsUtil.setValue(PrefsUtil.KEY_SCHEME_URL, intent.getData().toString());
        }

        if (action != null && Intent.ACTION_VIEW.equals(action) && intentData != null && intentData.contains("blockchain")) {
            mPrefsUtil.setValue(PrefsUtil.KEY_METADATA_URI, intentData);
        }

        if (intent.hasExtra(EXTRA_CONTACT_ACCEPTED)) {
            mPrefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true);
        }

        boolean isPinValidated = false;
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED);
        }

        boolean hasLoggedOut = mPrefsUtil.getValue(PrefsUtil.LOGGED_OUT, false);

        if (mPrefsUtil.getValue(PrefsUtil.KEY_GUID, "").isEmpty()) {
            // No GUID? Treat as new installation
            mDataListener.onNoGuid();

        } else if (hasLoggedOut) {
            // User has logged out recently. Show password reentry page
            mDataListener.onReEnterPassword();

        } else if (mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty()) {
            // No PIN ID? Treat as installed app without confirmed PIN
            mDataListener.onRequestPin();

        } else if (!mAppUtil.isSane()) {
            // Installed app, check sanity
            mDataListener.onCorruptPayload();

        } else if (isPinValidated && !mPayloadDataManager.getWallet().isUpgraded()) {
            // Legacy app has not been prompted for upgrade
            mAccessState.setIsLoggedIn(true);
            mDataListener.onRequestUpgrade();

        } else if (isPinValidated || (mAccessState.isLoggedIn())) {
            // App has been PIN validated
            mAccessState.setIsLoggedIn(true);
            if (!mPrefsUtil.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
                mDataListener.onStartMainActivity();
            } else {
                mDataListener.onStartOnboarding();
            }
        } else {
            mDataListener.onRequestPin();
        }
    }

    @NonNull
    public AppUtil getAppUtil() {
        return mAppUtil;
    }

}
