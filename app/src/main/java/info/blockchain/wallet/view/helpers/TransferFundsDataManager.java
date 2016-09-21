package info.blockchain.wallet.view.helpers;

import android.support.v4.util.Pair;

import info.blockchain.api.Unspent;
import info.blockchain.wallet.cache.DynamicFeeCache;
import info.blockchain.wallet.model.ItemAccount;
import info.blockchain.wallet.model.PendingTransaction;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadBridge;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SweepBundle;
import info.blockchain.wallet.payment.data.UnspentOutputs;
import info.blockchain.wallet.rxjava.RxUtil;
import info.blockchain.wallet.send.SendCoins;
import info.blockchain.wallet.util.CharSequenceX;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

public class TransferFundsDataManager {

    private PayloadManager mPayloadManager;

    public TransferFundsDataManager(PayloadManager payloadManager) {
        mPayloadManager = payloadManager;
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to default account.
     *
     * @return Returns a Map which bundles together the List of {@link PendingTransaction} objects,
     * as well as a Pair which contains the total to send and the total fees, in that order.
     */
    public Observable<Map<List<PendingTransaction>, Pair<Long, Long>>> getTransferableFundTransactionList(int addressToReceiveIndex) {
        return Observable.fromCallable(() -> {
                    Payment payment = new Payment();
                    BigInteger suggestedFeePerKb = DynamicFeeCache.getInstance().getSuggestedFee().defaultFeePerKb;
                    List<PendingTransaction> pendingTransactionList = new ArrayList<>();
                    List<LegacyAddress> legacyAddresses = mPayloadManager.getPayload().getLegacyAddresses();

                    long totalToSend = 0L;
                    long totalFee = 0L;

                    for (LegacyAddress legacyAddress : legacyAddresses) {

                        if (!legacyAddress.isWatchOnly() && MultiAddrFactory.getInstance().getLegacyBalance(legacyAddress.getAddress()) > 0) {
                            JSONObject unspentResponse = new Unspent().getUnspentOutputs(legacyAddress.getAddress());
                            if (unspentResponse != null) {
                                UnspentOutputs coins = payment.getCoins(unspentResponse);
                                SweepBundle sweepBundle = payment.getSweepBundle(coins, suggestedFeePerKb);

                                // Don't sweep if there are still unconfirmed funds in address
                                if (coins.getNotice() == null && sweepBundle.getSweepAmount().compareTo(SendCoins.bDust) == 1) {
                                    PendingTransaction pendingSpend = new PendingTransaction();
                                    pendingSpend.unspentOutputBundle = payment.getSpendableCoins(coins, sweepBundle.getSweepAmount(), suggestedFeePerKb);
                                    pendingSpend.sendingObject = new ItemAccount(legacyAddress.getLabel(), "", "", legacyAddress);
                                    pendingSpend.bigIntFee = pendingSpend.unspentOutputBundle.getAbsoluteFee();
                                    pendingSpend.bigIntAmount = sweepBundle.getSweepAmount();
                                    // assign new receive address for each transfer
                                    pendingSpend.receivingAddress = mPayloadManager.getReceiveAddress(addressToReceiveIndex);
                                    totalToSend += pendingSpend.bigIntAmount.longValue();
                                    totalFee += pendingSpend.bigIntFee.longValue();
                                    pendingTransactionList.add(pendingSpend);
                                }
                            }
                        }
                    }

                    Map<List<PendingTransaction>, Pair<Long, Long>> map = new HashMap<>();
                    map.put(pendingTransactionList, new Pair<>(totalToSend, totalFee));
                    return map;
                }
        ).compose(RxUtil.applySchedulers());
    }

    public Observable<Map<List<PendingTransaction>, Pair<Long, Long>>> getTransferableFundTransactionListForDefaultAccount() {
        return getTransferableFundTransactionList(mPayloadManager.getPayload().getHdWallet().getDefaultIndex());
    }

    /**
     * Takes a list of {@link PendingTransaction} objects and transfers them all to the default account.
     * Emits a String which is the Tx hash for each successful payment, and calls onCompleted when
     * all PendingTransactions have been finished successfully.
     *
     * @param pendingTransactions   A list of {@link PendingTransaction} objects
     * @param secondPassword        The double encryption password if necessary
     * @return                      An {@link Observable<String>}
     */
    public Observable<String> sendPayment(List<PendingTransaction> pendingTransactions, CharSequenceX secondPassword) {
        return getPaymentObservable(pendingTransactions, secondPassword)
                .compose(RxUtil.applySchedulers());
    }

    private Observable<String> getPaymentObservable(List<PendingTransaction> pendingTransactions, CharSequenceX secondPassword) {
        return Observable.create(subscriber -> {
            for (int i = 0; i < pendingTransactions.size(); i++) {
                PendingTransaction pendingTransaction = pendingTransactions.get(i);

                boolean isWatchOnly = false;

                LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.accountObject);
                String changeAddress = legacyAddress.getAddress();
                isWatchOnly = legacyAddress.isWatchOnly();

                final int finalI = i;
                try {
                    new Payment().submitPayment(pendingTransaction.unspentOutputBundle,
                            null,
                            legacyAddress,
                            pendingTransaction.receivingAddress,
                            changeAddress,
                            pendingTransaction.note,
                            pendingTransaction.bigIntFee,
                            pendingTransaction.bigIntAmount,
                            isWatchOnly,
                            secondPassword != null ? secondPassword.toString() : null,
                            new Payment.SubmitPaymentListener() {
                                @Override
                                public void onSuccess(String s) {
                                    subscriber.onNext(s);
                                    MultiAddrFactory.getInstance().setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() - (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue()));

                                    if (finalI == pendingTransactions.size() - 1) {
                                        PayloadBridge.getInstance().remoteSaveThread(null);
                                        subscriber.onCompleted();
                                    }
                                }

                                @Override
                                public void onFail(String error) {
                                    subscriber.onError(new Throwable(error));
                                }
                            });
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Boolean> savePayloadToServer() {
        return Observable.fromCallable(() -> mPayloadManager.savePayloadToServer())
                .compose(RxUtil.applySchedulers());
    }
}
