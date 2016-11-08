package piuk.blockchain.android.data.api;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import info.blockchain.api.PersistentUrls;

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class UrlSettings {

    public static final String KEY_CURRENT_ENVIRONMENT = "current_environment";

    private static final String TAG = UrlSettings.class.getSimpleName();
    @VisibleForTesting static final String KEY_ENV_PROD = "env_prod";
    @VisibleForTesting static final String KEY_ENV_STAGING = "env_staging";
    @VisibleForTesting static final String KEY_ENV_DEV = "env_dev";

    private Environment environment;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AppUtil appUtil;
    @Inject protected PersistentUrls persistentUrls;

    public UrlSettings() {
        Injector.getInstance().getAppComponent().inject(this);

        // Restore saved environment
        String storedEnv = prefsUtil.getValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD);
        if (Environment.fromString(storedEnv) != null) {
            setEnvironment(Environment.fromString(storedEnv));
        } else {
            // Set default if empty or malformed for some reason
            setEnvironment(Environment.PRODUCTION);
        }
    }

    public boolean shouldShowDebugMenu() {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD;
    }

    @NonNull
    public Environment getCurrentEnvironment() {
        return environment;
    }

    /**
     * Sets the current environment to whatever is passed to it. Clears all user data other than the
     * selected env and restarts the app
     *
     * @param environment The new {@link Environment} to switch to
     */
    public void changeEnvironment(@NonNull Environment environment) {
        setEnvironment(environment);
        appUtil.clearCredentialsAndKeepEnvironment();
    }

    private void setEnvironment(Environment environment) {
        this.environment = environment;
        switch (environment) {
            case PRODUCTION:
                persistentUrls.setProductionEnvironment();
                break;
            case STAGING:
                persistentUrls.setAddressInfoUrl(BuildConfig.STAGING_ADDRESS_INFO);
                persistentUrls.setBalanceUrl(BuildConfig.STAGING_BALANCE);
                persistentUrls.setDynamicFeeUrl(BuildConfig.STAGING_DYNAMIC_FEE);
                persistentUrls.setMultiAddressUrl(BuildConfig.STAGING_MULTIADDR_URL);
                persistentUrls.setPinstoreUrl(BuildConfig.STAGING_PIN_STORE_URL);
                persistentUrls.setSettingsUrl(BuildConfig.STAGING_SETTINGS_PAYLOAD_URL);
                persistentUrls.setTransactionDetailsUrl(BuildConfig.STAGING_TRANSACTION_URL);
                persistentUrls.setUnspentUrl(BuildConfig.STAGING_UNSPENT_OUTPUTS_URL);
                persistentUrls.setWalletPayloadUrl(BuildConfig.STAGING_WALLET_PAYLOAD_URL);
                persistentUrls.setCurrentEnvironment(PersistentUrls.Environment.STAGING);
                break;
            case DEV:
                persistentUrls.setAddressInfoUrl(BuildConfig.DEV_ADDRESS_INFO);
                persistentUrls.setBalanceUrl(BuildConfig.DEV_BALANCE);
                persistentUrls.setDynamicFeeUrl(BuildConfig.DEV_DYNAMIC_FEE);
                persistentUrls.setMultiAddressUrl(BuildConfig.DEV_MULTIADDR_URL);
                persistentUrls.setPinstoreUrl(BuildConfig.DEV_PIN_STORE_URL);
                persistentUrls.setSettingsUrl(BuildConfig.DEV_SETTINGS_PAYLOAD_URL);
                persistentUrls.setTransactionDetailsUrl(BuildConfig.DEV_TRANSACTION_URL);
                persistentUrls.setUnspentUrl(BuildConfig.DEV_UNSPENT_OUTPUTS_URL);
                persistentUrls.setWalletPayloadUrl(BuildConfig.DEV_WALLET_PAYLOAD_URL);
                persistentUrls.setCurrentEnvironment(PersistentUrls.Environment.DEV);
                break;
        }

        storeEnvironment(environment);
        Log.d(TAG, "setEnvironment: " + environment.getName());
    }

    private void storeEnvironment(Environment environment) {
        prefsUtil.setValue(KEY_CURRENT_ENVIRONMENT, environment.getName());
    }

    public enum Environment {

        PRODUCTION(KEY_ENV_PROD),
        STAGING(KEY_ENV_STAGING),
        DEV(KEY_ENV_DEV);

        private String name;

        Environment(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Environment fromString(String text) {
            if (text != null) {
                for (Environment environment : Environment.values()) {
                    if (text.equalsIgnoreCase(environment.getName())) {
                        return environment;
                    }
                }
            }
            return null;
        }
    }

}
