package info.blockchain.wallet.view.helpers;

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

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class TransferFundsHelper {

    private PayloadManager mPayloadManager;
    private long mTotalToSend = 0;
    private long mTotalFee = 0;

    public TransferFundsHelper(PayloadManager payloadManager) {
        mPayloadManager = payloadManager;
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to default account.
     * Returns a list of {@link PendingTransaction} if there are funds to be moved
     *
     * @return A list of PendingTransaction objects
     */
    public Observable<List<PendingTransaction>> getTransferableFundTransactionList() {
        return Observable.fromCallable(() -> {
                    Payment payment = new Payment();
                    BigInteger suggestedFeePerKb = DynamicFeeCache.getInstance().getSuggestedFee().defaultFeePerKb;
                    List<PendingTransaction> pendingTransactionList = new ArrayList<>();

                    int defaultIndex = mPayloadManager.getPayload().getHdWallet().getDefaultIndex();

                    List<LegacyAddress> legacyAddresses = mPayloadManager.getPayload().getLegacyAddresses();
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
                                    pendingSpend.receivingAddress = mPayloadManager.getReceiveAddress(defaultIndex);
                                    mTotalToSend += pendingSpend.bigIntAmount.longValue();
                                    mTotalFee += pendingSpend.bigIntFee.longValue();
                                    pendingTransactionList.add(pendingSpend);
                                }
                            }
                        }
                    }
                    return pendingTransactionList;
                }
        ).compose(RxUtil.applySchedulers());
    }

    public long getTotalToSend() {
        return mTotalToSend;
    }

    public long getTotalFee() {
        return mTotalFee;
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
    public Observable<String> sendPayment(List<PendingTransaction> pendingTransactions, String secondPassword) {
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
                            secondPassword,
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
}
