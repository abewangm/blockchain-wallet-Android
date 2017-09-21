package piuk.blockchain.android.data.ethereum

import com.subgraph.orchid.encoders.Hex
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.data.EthLatestBlock
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Completable
import io.reactivex.Observable
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.web3j.protocol.core.methods.request.RawTransaction
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxPinning
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.util.annotations.Mockable
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import java.util.concurrent.Callable

@Mockable
class EthDataManager(
        private val payloadManager: PayloadManager,
        private val ethAccountApi: EthAccountApi,
        private val ethDataStore: EthDataStore,
        rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    /**
     * Clears the currently stored ETH account and [EthAddressResponse] from memory.
     */
    fun clearEthAccountDetails() = ethDataStore.clearEthData()

    /**
     * Returns an [EthAddressResponse] object for a given ETH address as an [Observable]. An
     * [CombinedEthModel] contains a list of transactions associated with the account, as well
     * as a final balance. Calling this function also caches the [CombinedEthModel].
     *
     * @return An [Observable] wrapping an [CombinedEthModel]
     */
    fun fetchEthAddress(): Observable<CombinedEthModel> = rxPinning.call<CombinedEthModel> {
        ethAccountApi.getEthAddress(listOf(ethDataStore.ethWallet!!.account.address))
                .map(::CombinedEthModel)
                .doOnNext { ethDataStore.ethAddressResponse = it }
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns the user's ETH account object if previously fetched.
     *
     * @return A nullable [CombinedEthModel] object
     */
    fun getEthResponseModel() = ethDataStore.ethAddressResponse

    /**
     * Returns the user's [EthereumWallet] object if previously fetched.
     *
     * @return A nullable [EthereumWallet] object
     */
    fun getEthWallet() = ethDataStore.ethWallet

    /**
     * Returns a steam of [EthTransaction] objects associated with a user's ETH address specifically
     * for displaying in the transaction list. These are cached and may be empty if the account
     * hasn't previously been fetched.
     *
     * @return An [Observable] stream of [EthTransaction] objects
     */
    fun getEthTransactions(): Observable<EthTransaction> {
        ethDataStore.ethAddressResponse?.let {
            return Observable.just(it)
                    .flatMapIterable { it.getTransactions() }
                    .compose(RxUtil.applySchedulersToObservable())
        }

        return Observable.empty()
    }

    /**
     * Returns a [EthLatestBlock] object which contains information about the most recently
     * mined block.
     *
     * @return An [Observable] wrapping an [EthLatestBlock]
     */
    fun getLatestBlock(): Observable<EthLatestBlock> = rxPinning.call<EthLatestBlock> {
        ethAccountApi.latestBlock
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns true if a given ETH address is associated with an Ethereum contract, which is
     * currently unsupported. This should be used to validate any proposed destination address for
     * funds.
     *
     * @param address The ETH address to be queried
     * @return An [Observable] returning true or false based on the address's contract status
     */
    fun getIfContract(address: String): Observable<Boolean> = rxPinning.call<Boolean> {
        ethAccountApi.getIfContract(address)
                .compose(RxUtil.applySchedulersToObservable())
    }

    /**
     * Returns the transaction notes for a given transaction hash, or null if not found.
     */
    fun getTransactionNotes(hash: String) = ethDataStore.ethWallet?.txNotes?.get(hash)

    /**
     * Puts a given note in the [HashMap] of transaction notes keyed to a transaction hash. This
     * information is then saved in the metadata service.
     *
     * @return A [Completable] object
     */
    fun updateTransactionNotes(hash: String, note: String): Completable = rxPinning.call {
        Completable.fromCallable {
            if (ethDataStore.ethWallet != null) {
                ethDataStore.ethWallet?.let {
                    it.txNotes.put(hash, note)
                    it.save()
                }
                return@fromCallable Void.TYPE
            } else {
                throw IllegalStateException("ETH Wallet is null")
            }
        }
    }.compose(RxUtil.applySchedulersToCompletable())

    /**
     * Returns EthereumWallet stored in metadata. If metadata entry doesn't exists it will be inserted.
     *
     * @param defaultLabel The ETH address default label to be used if metadata entry doesn't exist
     * @return An [Observable] returning EthereumWallet
     */
    fun initEthereumWallet(
            metadataNode: DeterministicKey,
            defaultLabel: String
    ): Observable<EthereumWallet> = rxPinning.call<EthereumWallet> {
        Observable.fromCallable { fetchOrCreateEthereumWallet(metadataNode, defaultLabel) }
                .doOnNext { ethDataStore.ethWallet = it }
                .compose(RxUtil.applySchedulersToObservable())
    }

    private fun fetchOrCreateEthereumWallet(
            metadataNode: DeterministicKey,
            defaultLabel: String
    ): EthereumWallet {

        var ethWallet = EthereumWallet.load(metadataNode)

        if (ethWallet == null || ethWallet.account == null || !ethWallet.account.isCorrect) {
            val masterKey = payloadManager.payload.hdWallets[0].masterKey
            ethWallet = EthereumWallet(masterKey, defaultLabel)
            ethWallet.save()
        }

        return ethWallet
    }

    /**
     * @param gasPrice Represents the fee the sender is willing to pay for gas. One unit of gas
     *                 corresponds to the execution of one atomic instruction, i.e. a computational step
     * @param gasLimit Represents the maximum number of computational steps the transaction
     *                 execution is allowed to take
     * @param weiValue The amount of wei to transfer from the sender to the recipient
     */
    fun createEthTransaction(
            nonce: BigInteger,
            to: String,
            gasPrice: BigInteger,
            gasLimit: BigInteger,
            weiValue: BigInteger
    ): RawTransaction? = RawTransaction.createEtherTransaction(
            nonce,
            gasPrice,
            gasLimit,
            to,
            weiValue)

    fun signEthTransaction(rawTransaction: RawTransaction, ecKey: ECKey): Observable<ByteArray> =
            Observable.fromCallable {
                ethDataStore.ethWallet!!.account!!.signTransaction(rawTransaction, ecKey)
            }

    fun pushEthTx(signedTxBytes: ByteArray): Observable<String> =
            ethAccountApi.pushTx("0x" + String(Hex.encode(signedTxBytes)))
                    .compose(RxUtil.applySchedulersToObservable())

    fun setLastTxHashComplatable(txHash: String) = Observable
            .fromCallable(Callable { setLastTxHash(txHash) })
            .compose(RxUtil.applySchedulersToObservable())

    private fun setLastTxHash(txHash: String): String {
        ethDataStore.ethWallet!!.lastTransactionHash = txHash
        ethDataStore.ethWallet!!.save()

        return txHash;
    }
}