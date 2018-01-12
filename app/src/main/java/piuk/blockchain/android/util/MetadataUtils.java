package piuk.blockchain.android.util;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;

import java.io.IOException;

import info.blockchain.wallet.BitcoinCashWallet;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;

/**
 * Simple wrapper class to allow mocking of metadata keys
 */
public class MetadataUtils {

    public MetadataUtils() {
    }

    public Metadata getMetadataNode(DeterministicKey metaDataHDNode, int type) throws IOException, MetadataException {
        return new Metadata.Builder(metaDataHDNode, type).build();
    }
}
