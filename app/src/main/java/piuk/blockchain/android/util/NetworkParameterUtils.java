package piuk.blockchain.android.util;

import info.blockchain.wallet.api.PersistentUrls;

import org.bitcoinj.core.NetworkParameters;

/**
 * Simple wrapper class to allow mocking of coin network parameters
 */
@Deprecated // Use {@link EnvironmentSettings which should be perfectly mockable
public class NetworkParameterUtils {

    public NetworkParameterUtils() {
    }

    public NetworkParameters getBitcoinCashParams() {
        return PersistentUrls.getInstance().getBitcoinCashParams();
    }

    public NetworkParameters getBitcoinParams() {
        return PersistentUrls.getInstance().getBitcoinParams();
    }

}
