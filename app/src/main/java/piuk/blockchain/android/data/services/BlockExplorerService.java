package piuk.blockchain.android.data.services;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.data.Address;
import info.blockchain.api.data.Transaction;
import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.Payment;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import java.util.Arrays;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class BlockExplorerService {

    // TODO: 28/02/2017 WIP - Error handling, currently assuming execute is successful
    private BlockExplorer blockExplorer;

    public BlockExplorerService(BlockExplorer blockExplorer) {
        this.blockExplorer = blockExplorer;
    }

    /**
     * Returns the current balance for a given {@link LegacyAddress}
     *
     * @param address   The address which you want to know the balance of
     * @param limit
     * @param offset
     * @return          An {@link Observable <Address>}
     */
    public Observable<Address> getAddressBalance(String address, int limit, int offset) {
        return Observable.fromCallable(() -> blockExplorer.getAddress(address, limit, offset))
                .flatMap(call -> Observable.fromCallable(() -> call.execute().body()))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns the transaction details for a given hash
     *
     * @param hash   The transaction hash
     * @return          An {@link Observable <Transaction>}
     */
    public Observable<Transaction> getTransactionDetailsFromHash(String hash) {
        return Observable.fromCallable(() -> blockExplorer.getTransactionDetails(hash))
            .flatMap(call -> Observable.fromCallable(() -> call.execute().body()))
            .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Get an {@link UnspentOutputs} object for a given address as a string
     * @param address
     * @return {@link Observable<UnspentOutputs>}
     */
    public Observable<UnspentOutputs> getUnspentOutputs(String address) {
        return Observable.fromCallable(() -> blockExplorer.getUnspentOutputs(Arrays.asList(address)))
            .flatMap(call -> Observable.fromCallable(() -> call.execute().body()))
            .compose(RxUtil.applySchedulersToObservable());
    }
}
