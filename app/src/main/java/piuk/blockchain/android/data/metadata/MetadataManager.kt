package piuk.blockchain.android.data.metadata

import com.google.common.base.Optional
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.metadata.MetadataNodeFactory
import io.reactivex.Completable
import io.reactivex.Observable
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.MetadataUtils
import piuk.blockchain.android.util.annotations.Mockable

/**
 * Manages metadata nodes/keys derived from a user's wallet credentials.
 * This helps to avoid repeatedly asking user for second password.
 *
 * There are currently 2 nodes/keys (serialized privB58):
 * sharedMetadataNode   - used for inter-wallet communication
 * metadataNode         - used for storage
 *
 * The above nodes/keys can be derived from a user's master private key.
 * After these keys have been derived we store them on the metadata service with a node/key
 * derived from 'guid + sharedkey + wallet password'. This will allow us to retrieve these derived
 * keys with just a user's credentials and not derive them again.
 *
 */
@Mockable
class MetadataManager(
        private val payloadDataManager: PayloadDataManager,
        private val metadataUtils: MetadataUtils,
        rxBus: RxBus
) {
    private val rxPinning = RxPinning(rxBus)

    fun attemptMetadataSetup() = initMetadataNodesObservable()

    fun decryptAndSetupMetadata(secondPassword: String): Completable {
        payloadDataManager.decryptHDWallet(secondPassword)
        return payloadDataManager.generateNodes()
                .andThen(Completable.fromObservable(initMetadataNodesObservable()))
    }

    fun fetchMetadata(metadataType: Int): Observable<Optional<String>> = rxPinning.call<Optional<String>> {
        payloadDataManager.metadataNodeFactory.map { nodeFactory ->
            metadataUtils.getMetadataNode(nodeFactory.metadataNode, metadataType).metadataOptional
        }
    }.compose(RxUtil.applySchedulersToObservable<Optional<String>>())

    fun saveToMetadata(data: String, metadataType: Int): Completable = rxPinning.call {
        payloadDataManager.metadataNodeFactory.flatMapCompletable {
            Completable.fromCallable {
                metadataUtils.getMetadataNode(it.metadataNode, metadataType).putMetadata(data)
            }
        }.compose(RxUtil.applySchedulersToCompletable())
    }

    /**
     * Loads or derives the stored nodes/keys from the metadata service.
     *
     * @throws InvalidCredentialsException If nodes/keys cannot be derived because wallet is double encrypted
     */
    private fun initMetadataNodesObservable(): Observable<MetadataNodeFactory> = rxPinning.call<MetadataNodeFactory> {
        payloadDataManager.loadNodes()
                .map { loaded ->
                    if (!loaded) {
                        if (payloadDataManager.isDoubleEncrypted) {
                            throw InvalidCredentialsException("Unable to derive metadata keys, payload is double encrypted")
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
                .flatMap { needsGeneration ->
                    if (needsGeneration) {
                        payloadDataManager.generateAndReturnNodes()
                    } else {
                        payloadDataManager.metadataNodeFactory
                    }
                }
    }.compose(RxUtil.applySchedulersToObservable())
}