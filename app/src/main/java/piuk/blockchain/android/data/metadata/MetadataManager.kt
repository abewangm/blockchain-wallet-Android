package piuk.blockchain.android.data.metadata

import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.Completable
import org.bitcoinj.crypto.DeterministicKey
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.android.util.StringUtils
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
        private val ethDataManager: EthDataManager,
        private val bchDataManager: BchDataManager,
        private val shapeShiftDataManager: ShapeShiftDataManager,
        private val stringUtils: StringUtils
) {

    fun attemptMetadataSetup(): Completable {
        return initMetadataNodesObservable(null)
    }

    fun generateAndSetupMetadata(secondPassword: String): Completable {
        payloadDataManager.generateNodes(secondPassword)
        return initMetadataNodesObservable(secondPassword)
    }

    /**
     * Loads or derives the stored nodes/keys from the metadata service.
     *
     * @throws InvalidCredentialsException If nodes/keys cannot be derived because wallet is double encrypted
     */
    private fun initMetadataNodesObservable(secondPassword: String?): Completable {

        return payloadDataManager.loadNodes()
                .map { loaded ->
                    if(!loaded) {
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
                    if(needsGeneration) {
                        payloadDataManager.generateAndReturnNodes(secondPassword)
                    } else {
                        payloadDataManager.metadataNodeFactory
                    }
                }
                .flatMapCompletable {
                    loadMetadataElements(it.metadataNode)
                }
                .compose(RxUtil.applySchedulersToCompletable())
    }

    private fun loadMetadataElements(metadataNode: DeterministicKey): Completable {
        return ethDataManager.initEthereumWallet(metadataNode, stringUtils.getString(R.string.eth_default_account_label))
                .andThen(bchDataManager.initBchWallet(metadataNode, stringUtils.getString(R.string.bch_default_account_label))
                        .andThen(Completable.fromObservable(shapeShiftDataManager.initShapeshiftTradeData(metadataNode))))
    }
}