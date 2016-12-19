package piuk.blockchain.android.data.api;

import android.support.annotation.NonNull;
import android.util.Log;

import info.blockchain.api.PersistentUrls;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static info.blockchain.api.PersistentUrls.Environment.DEV;
import static info.blockchain.api.PersistentUrls.Environment.STAGING;
import static info.blockchain.api.PersistentUrls.KEY_ENV_PROD;

@SuppressWarnings("WeakerAccess")
public class UrlSettings {

    public static final String KEY_CURRENT_ENVIRONMENT = "current_environment";

    private static final String TAG = UrlSettings.class.getSimpleName();

    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AppUtil appUtil;
    @Inject protected PersistentUrls persistentUrls;

    public UrlSettings() {
        Injector.getInstance().getAppComponent().inject(this);

        // Restore saved environment
        String storedEnv = prefsUtil.getValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD);
        if (PersistentUrls.Environment.fromString(storedEnv) != null) {
            setEnvironment(PersistentUrls.Environment.fromString(storedEnv));
        } else {
            // Set default if empty or malformed for some reason
            setEnvironment(PersistentUrls.Environment.PRODUCTION);
        }
    }

    public boolean shouldShowDebugMenu() {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD;
    }

    @NonNull
    public PersistentUrls.Environment getCurrentEnvironment() {
        return persistentUrls.getCurrentEnvironment();
    }

    @NonNull
    public String getBaseServerUrl() {
        return PersistentUrls.getInstance().getCurrentBaseServerUrl();
    }

    @NonNull
    public String getBaseApiUrl() {
        return PersistentUrls.getInstance().getCurrentBaseApiUrl();
    }

    /**
     * Sets the current environment to whatever is passed to it. Clears all user data other than the
     * selected env and restarts the app
     *
     * @param environment The new {@link PersistentUrls.Environment} to switch to
     */
    public void changeEnvironment(@NonNull PersistentUrls.Environment environment) {
        setEnvironment(environment);
        appUtil.clearCredentialsAndKeepEnvironment();
    }

    private void setEnvironment(PersistentUrls.Environment environment) {
        switch (environment) {
            case PRODUCTION:
                persistentUrls.setProductionEnvironment();
                break;
            case STAGING:
                persistentUrls.setCurrentApiUrl(BuildConfig.STAGING_API_SERVER);
                persistentUrls.setCurrentServerUrl(BuildConfig.STAGING_BASE_SERVER);
                persistentUrls.setCurrentEnvironment(STAGING);
                break;
            case DEV:
                persistentUrls.setCurrentApiUrl(BuildConfig.DEV_API_SERVER);
                persistentUrls.setCurrentServerUrl(BuildConfig.DEV_BASE_SERVER);
                persistentUrls.setCurrentEnvironment(DEV);
                break;
        }

        storeEnvironment(environment);
        Log.d(TAG, "setEnvironment: " + environment.getName());
    }

    private void storeEnvironment(PersistentUrls.Environment environment) {
        prefsUtil.setValue(KEY_CURRENT_ENVIRONMENT, environment.getName());
    }

}
