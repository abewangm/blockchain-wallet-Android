package piuk.blockchain.android.data.api;

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
    private static final String KEY_ENV_PROD = "env_prod";
    private static final String KEY_ENV_STAGING = "env_staging";
    private static final String KEY_ENV_DEV = "env_dev";

    private static UrlSettings urlSettings;
    private Environment environment;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected AppUtil appUtil;

    {
        Injector.getInstance().getAppComponent().inject(this);

        // Restore saved environment
        String storedEnv = prefsUtil.getValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD);
        if (Environment.fromString(storedEnv) != null) {
            setEnvironment(Environment.fromString(storedEnv));
        } else {
            // Set default if empty
            setEnvironment(Environment.PRODUCTION);
        }
    }

    private UrlSettings() {
        // Empty Constructor
    }

    public static UrlSettings getInstance() {
        if (urlSettings == null) {
            urlSettings = new UrlSettings();
        }
        return urlSettings;
    }

    public boolean shouldShowDebugMenu() {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD;
    }

    public Environment getCurrentEnvironment() {
        return environment;
    }

    public void changeEnvironment(Environment environment) {
        setEnvironment(environment);
        appUtil.clearCredentialsAndKeepEnvironment();
    }

    private void setEnvironment(Environment environment) {
        this.environment = environment;
        switch (environment) {
            case PRODUCTION:
                PersistentUrls.getInstance().setProductionEnvironment();
                break;
            case STAGING:
                PersistentUrls.getInstance().setAddressInfoUrl(BuildConfig.STAGING_ADDRESS_INFO);
                PersistentUrls.getInstance().setBalanceUrl(BuildConfig.STAGING_BALANCE);
                PersistentUrls.getInstance().setDynamicFeeUrl(BuildConfig.STAGING_DYNAMIC_FEE);
                PersistentUrls.getInstance().setMultiAddressUrl(BuildConfig.STAGING_MULTIADDR_URL);
                PersistentUrls.getInstance().setPinstoreUrl(BuildConfig.STAGING_PIN_STORE_URL);
                PersistentUrls.getInstance().setSettingsUrl(BuildConfig.STAGING_SETTINGS_PAYLOAD_URL);
                PersistentUrls.getInstance().setTransactionDetailsUrl(BuildConfig.STAGING_TRANSACTION_URL);
                PersistentUrls.getInstance().setUnspentUrl(BuildConfig.STAGING_UNSPENT_OUTPUTS_URL);
                PersistentUrls.getInstance().setWalletPayloadUrl(BuildConfig.STAGING_WALLET_PAYLOAD_URL);
                break;
            case DEV:
                PersistentUrls.getInstance().setAddressInfoUrl(BuildConfig.DEV_ADDRESS_INFO);
                PersistentUrls.getInstance().setBalanceUrl(BuildConfig.DEV_BALANCE);
                PersistentUrls.getInstance().setDynamicFeeUrl(BuildConfig.DEV_DYNAMIC_FEE);
                PersistentUrls.getInstance().setMultiAddressUrl(BuildConfig.DEV_MULTIADDR_URL);
                PersistentUrls.getInstance().setPinstoreUrl(BuildConfig.DEV_PIN_STORE_URL);
                PersistentUrls.getInstance().setSettingsUrl(BuildConfig.DEV_SETTINGS_PAYLOAD_URL);
                PersistentUrls.getInstance().setTransactionDetailsUrl(BuildConfig.DEV_TRANSACTION_URL);
                PersistentUrls.getInstance().setUnspentUrl(BuildConfig.DEV_UNSPENT_OUTPUTS_URL);
                PersistentUrls.getInstance().setWalletPayloadUrl(BuildConfig.DEV_WALLET_PAYLOAD_URL);
                break;
        }

        storeEnvironment(environment);
        Log.d(TAG, "setEnvironment: " + environment.getName());
    }

    private void storeEnvironment(Environment environment) {
        switch (environment) {
            case PRODUCTION:
                prefsUtil.setValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD);
                break;
            case STAGING:
                prefsUtil.setValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_STAGING);
                break;
            case DEV:
                prefsUtil.setValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_DEV);
                break;
        }
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
