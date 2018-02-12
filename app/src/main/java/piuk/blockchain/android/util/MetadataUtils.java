package piuk.blockchain.android.util;

import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;

import org.bitcoinj.crypto.DeterministicKey;

import java.io.IOException;

/**
 * Simple wrapper class to allow mocking of metadata keys
 */
public class MetadataUtils {

    public Metadata getMetadataNode(DeterministicKey metaDataHDNode, int type) throws IOException, MetadataException {
        return new Metadata.Builder(metaDataHDNode, type).build();
    }

}
