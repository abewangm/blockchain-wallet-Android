package piuk.blockchain.android.data.datamanagers;

import com.google.common.base.Optional;

import info.blockchain.wallet.metadata.Metadata;

import org.bitcoinj.crypto.DeterministicKey;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.MetadataService;

public class MetadataManager {

    private MetadataService metadataService;

    public MetadataManager(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    /**
     * Sets the node for the metadata service. The service will crash without it. Can return and
     * error which will need to be handled.
     *
     * @param deterministicKey A {@link DeterministicKey}, see {@link info.blockchain.wallet.payload.PayloadManager#getMasterKey()}
     * @param type             An int specifying the metadata type
     * @param encrypted        A boolean flag enabling or disabling encryption
     * @return A Completable object, ie an asynchronous void operation
     * @see Metadata
     */
    public Completable setMetadataNode(DeterministicKey deterministicKey, int type, boolean encrypted) {
        return metadataService.setMetadataNode(deterministicKey, type, encrypted)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Puts a new piece of data into the metadata system.
     *
     * @param payload JSON data formatted as a String
     * @return A Completable object, ie an asynchronous void operation
     */
    public Completable putMetadata(String payload) {
        return metadataService.putMetadata(payload)
                .compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns a metadata entry. May return nothing.
     *
     * @return An Observable which contains an {@link Optional} object. The method to be called is
     * nullable, and therefore must be returned inside an Optional to prevent RxJava2 from crashing
     * with an NPE.
     */
    public Observable<Optional<String>> getMetadata() {
        return metadataService.getMetadata()
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Deletes a given metadata entry.
     *
     * @param payload JSON data formatted as a String
     * @return A Completable object, ie an asynchronous void operation
     */
    public Completable deleteMetaData(String payload) {
        return metadataService.deleteMetadata(payload)
                .compose(RxUtil.applySchedulersToCompletable());
    }

}
