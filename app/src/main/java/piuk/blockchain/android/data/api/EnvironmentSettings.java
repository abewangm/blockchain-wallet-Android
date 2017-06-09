package piuk.blockchain.android.data.api;

import android.support.annotation.NonNull;

import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.PersistentUrls;

import org.bitcoinj.params.AbstractBitcoinNetParams;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import piuk.blockchain.android.BuildConfig;

@SuppressWarnings("WeakerAccess")
public class EnvironmentSettings {

    public boolean shouldShowDebugMenu() {
        return BuildConfig.DEBUG || BuildConfig.DOGFOOD;
    }

    @NonNull
    public Environment getEnvironment() {
        return Environment.fromString(BuildConfig.ENVIRONMENT);
    }

    @NonNull
    public String getExplorerUrl() {
        return BuildConfig.EXPLORER_URL;
    }

    @NonNull
    public String getApiUrl() {
        return BuildConfig.API_URL;
    }

    @NonNull
    public String getWebsocketUrl() {
        return BuildConfig.WEBSOCKET_URL;
    }

    // TODO: 09/06/2017 Switch over to using BuildConfig once URLs finalised
    @NonNull
    public String getCurrentSFOXUrl() {
        return PersistentUrls.SFOX_URL;
    }

    // TODO: 09/06/2017 Switch over to using BuildConfig once URLs finalised
    @NonNull
    public String getCurrentCoinifyUrl() {
        return PersistentUrls.COINIFY_URL;
    }

    @NonNull
    public AbstractBitcoinNetParams getNetworkParameters() {
        switch (getEnvironment()) {
            case TESTNET:
                return TestNet3Params.get();
            case PRODUCTION:
            case DEV:
            case STAGING:
            default:
                return MainNetParams.get();
        }
    }
}
