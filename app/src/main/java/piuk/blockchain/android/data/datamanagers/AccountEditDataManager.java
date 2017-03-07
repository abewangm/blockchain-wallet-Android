package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.NotImplementedException;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.ui.send.PendingTransaction;

public class AccountEditDataManager {

    private PaymentService paymentService;
    private PayloadManager payloadManager;

    public AccountEditDataManager(PaymentService paymentService, PayloadManager payloadManager) {
        this.paymentService = paymentService;
        this.payloadManager = payloadManager;
    }

    /**
     * Generates a {@link PendingTransaction} object for a given legacy address, where the output is
     * the default account in the user's wallet
     *
     * @param legacyAddress The {@link LegacyAddress} you wish to transfer funds from
     * @param payment       A new {@link Payment} object
     * @return An {@link Observable<PendingTransaction>}
     */
    public Observable<PendingTransaction> getPendingTransactionForLegacyAddress(LegacyAddress legacyAddress,
                                                                                Payment payment) {
        // TODO: 21/02/2017
        throw new NotImplementedException("");
//        PendingTransaction pendingTransaction = new PendingTransaction();
//
//        return getUnspentOutputs(legacyAddress, payment)
//                .flatMap(unspentOutputs -> {
//                    BigInteger suggestedFeePerKb = new BigDecimal(DynamicFeeCache.getInstance().getCachedDynamicFee().getDefaultFee().getFee()).toBigInteger();
//
//                    Pair<BigInteger, BigInteger> sweepableCoins = Payment
//                        .getSweepableCoins(unspentOutputs, suggestedFeePerKb);
//                    BigInteger sweepAmount = sweepableCoins.getLeft();
//                    BigInteger absFeeForSweep = sweepableCoins.getRight();
//
//                    // To default account
//                    int defaultIndex = payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx();
//                    Account defaultAccount = payloadManager.getPayload().getHdWallets().get(0).getAccounts().get(defaultIndex);
//                    pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepAmount.toString(), "", sweepAmount.longValue(), legacyAddress);
//                    pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", sweepAmount.longValue(), defaultAccount);
//                    pendingTransaction.unspentOutputBundle = Payment.getSpendableCoins(unspentOutputs, sweepAmount, suggestedFeePerKb);
//                    pendingTransaction.bigIntAmount = sweepAmount;
//                    pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();
//
//                    return getNextReceiveAddress(defaultIndex);
//                })
//                .map(receivingAddress -> {
//                    pendingTransaction.receivingAddress = receivingAddress;
//                    return pendingTransaction;
//                })
//                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Submits a payment to a specified address and returns the transaction hash if successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys                A List of elliptic curve keys
     * @param toAddress           The address to send the funds to
     * @param changeAddress       A change address
     * @param bigIntFee           The specified fee amount
     * @param bigIntAmount        The actual transaction amount
     * @return An {@link Observable<String>} where the String is the transaction hash
     */
    public Observable<String> submitPayment(SpendableUnspentOutputs unspentOutputBundle,
                                            List<ECKey> keys,
                                            String toAddress,
                                            String changeAddress,
                                            BigInteger bigIntFee,
                                            BigInteger bigIntAmount) {

        return paymentService.submitPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount)
                .compose(RxUtil.applySchedulersToObservable());
    }

    // TODO: 21/10/2016 Move all PayloadManager methods out of here
    public Observable<Boolean> syncPayloadWithServer() {
        return Observable.fromCallable(() -> payloadManager.save())
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<String> getNextReceiveAddress(int defaultIndex) {
        Account account = payloadManager.getPayload().getHdWallets().get(0).getAccounts().get(defaultIndex);
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account));
    }

    /**
     * Returns {@link Completable} which updates balances and transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return {@link Completable}
     * @see IgnorableDefaultObserver
     */
    public Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.updateMultiAddress(null, 50, 0);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    // TODO: 21/02/2017
//    private Observable<UnspentOutputs> getUnspentOutputs(LegacyAddress legacyAddress, Payment payment) {
//        return unspentService.getUnspentOutputs(legacyAddress.getAddress(), payment);
//    }
}
