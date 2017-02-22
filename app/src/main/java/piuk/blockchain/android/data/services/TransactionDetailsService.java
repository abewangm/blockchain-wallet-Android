package piuk.blockchain.android.data.services;

import info.blockchain.wallet.transaction.Transaction;

import org.apache.commons.lang3.NotImplementedException;
import piuk.blockchain.android.data.rxjava.RxUtil;
import io.reactivex.Observable;

public class TransactionDetailsService {

    // TODO: 22/02/2017  
//    private TransactionDetails transactionDetails;
//
//    public TransactionDetailsService(TransactionDetails transactionDetails) {
//        this.transactionDetails = transactionDetails;
//    }

    /**
     * Get a specific {@link Transaction} from a hash
     *
     * @param hash The hash of the transaction to be returned
     * @return A Transaction object
     */
    public Observable<Transaction> getTransactionDetailsFromHash(String hash) {
        throw new NotImplementedException("todo");
        // TODO: 22/02/2017  
//        return Observable.fromCallable(() -> transactionDetails.getTransactionDetails(hash))
//                .compose(RxUtil.applySchedulersToObservable());
    }
}
