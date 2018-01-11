package piuk.blockchain.android.util;

import org.bitcoinj.core.NetworkParameters;

import info.blockchain.wallet.api.PersistentUrls;

/**
 * Simple wrapper class to allow mocking of coin network parameters
 */
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
