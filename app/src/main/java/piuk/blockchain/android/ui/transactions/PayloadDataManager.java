package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class PayloadDataManager {

    private PayloadManager payloadManager;

    public PayloadDataManager(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {
        return payloadManager.getLabelFromAddress(address);
    }

    /**
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    public Completable syncPayloadWithServer() {
        return Completable.fromCallable(() -> {
            payloadManager.save();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns {@link Completable} which updates balances and transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    public Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.updateAllTransactions(50, 0);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param defaultIndex The index of the account for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddress(int defaultIndex) {
        Account account = payloadManager.getPayload().getHdWallets().get(0).getAccounts().get(defaultIndex);
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account));
    }

    public Wallet getWallet() {
        return payloadManager.getPayload();
    }

    public int getDefaultAccountIndex() {
        return getWallet().getHdWallets().get(0).getDefaultAccountIdx();
    }

}
