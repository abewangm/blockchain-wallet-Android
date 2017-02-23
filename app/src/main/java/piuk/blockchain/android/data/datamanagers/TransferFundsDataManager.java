package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.Payment;

import java.math.BigDecimal;
import java.util.Arrays;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.ECKey;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.annotations.Thunk;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class TransferFundsDataManager {

    @Thunk PayloadManager payloadManager;
    @Thunk MultiAddrFactory multiAddrFactory;

    public TransferFundsDataManager(PayloadManager payloadManager, MultiAddrFactory multiAddrFactory) {
        this.payloadManager = payloadManager;
        this.multiAddrFactory = multiAddrFactory;
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of {@link PendingTransaction} objects with outputs set to an account defined by it's
     * index in the list of HD accounts.
     *
     * @param addressToReceiveIndex The index of the account to which you want to send the funds
     * @return Returns a Map which bundles together the List of {@link PendingTransaction} objects,
     * as well as a Pair which contains the total to send and the total fees, in that order.
     */
    public Observable<Triple<List<PendingTransaction>, Long, Long>> getTransferableFundTransactionList(
        int addressToReceiveIndex) {
        return Observable.fromCallable(() -> {

                BigInteger suggestedFeePerKb = new BigDecimal(
                    DynamicFeeCache.getInstance().getSuggestedFee().getDefaultFee().getFee())
                    .toBigInteger();

                List<PendingTransaction> pendingTransactionList = new ArrayList<>();
                List<LegacyAddress> legacyAddresses = payloadManager.getPayload()
                    .getLegacyAddressList();

                long totalToSend = 0L;
                long totalFee = 0L;

                for (LegacyAddress legacyAddress : legacyAddresses) {

                    if (!legacyAddress.isWatchOnly()
                        && multiAddrFactory.getLegacyBalance(legacyAddress.getAddress()) > 0) {

//                        JSONObject unspentResponse = unspentApi
//                            .getUnspentOutputs(legacyAddress.getAddress());
                        Response<UnspentOutputs> coins = Payment.getUnspentCoins(Arrays.asList(legacyAddress.getAddress())).execute();

                        // TODO: 22/02/2017
//                        if (unspentResponse != null) {
//
//                            Pair<BigInteger, BigInteger> sweepableCoins = Payment
//                                .getSweepableCoins(unspentOutputs, suggestedFeePerKb);
//                            BigInteger sweepAmount = sweepableCoins.getLeft();
//                            BigInteger absFeeForSweep = sweepableCoins.getRight();
//
//
//                            UnspentOutputs coins = Payment.getUnspentCoins(unspentResponse);
//                            SweepBundle sweepBundle = payment.getSweepBundle(coins, suggestedFeePerKb);
//
//                            // Don't sweep if there are still unconfirmed funds in address
//                            if (coins.getNotice() == null
//                                && sweepBundle.getSweepAmount().compareTo(SendCoins.bDust) == 1) {
//
//                                PendingTransaction pendingSpend = new PendingTransaction();
//                                pendingSpend.unspentOutputBundle = payment
//                                    .getSpendableCoins(coins, sweepBundle.getSweepAmount(),
//                                        suggestedFeePerKb);
//                                pendingSpend.sendingObject = new ItemAccount(legacyAddress.getLabel(),
//                                    "", "", null, legacyAddress);
//                                pendingSpend.bigIntFee = pendingSpend.unspentOutputBundle
//                                    .getAbsoluteFee();
//                                pendingSpend.bigIntAmount = sweepBundle.getSweepAmount();
//                                pendingSpend.addressToReceiveIndex = addressToReceiveIndex;
//                                totalToSend += pendingSpend.bigIntAmount.longValue();
//                                totalFee += pendingSpend.bigIntFee.longValue();
//                                pendingTransactionList.add(pendingSpend);
//                            }
//                        }
                    }
                }

                return Triple.of(pendingTransactionList, totalToSend, totalFee);
            }
        ).compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Check if there are any spendable legacy funds that need to be sent to a HD wallet. Constructs
     * a list of {@link PendingTransaction} objects with outputs set to the default HD account.
     *
     * @return Returns a Triple object which bundles together the List of {@link PendingTransaction}
     * objects, as well as the total to send and the total fees, in that order.
     */
    public Observable<Triple<List<PendingTransaction>, Long, Long>> getTransferableFundTransactionListForDefaultAccount() {
        return getTransferableFundTransactionList(payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx());
    }

    /**
     * Takes a list of {@link PendingTransaction} objects and transfers them all. Emits a String
     * which is the Tx hash for each successful payment, and calls onCompleted when all
     * PendingTransactions have been finished successfully.
     *
     * @param payment             A new {@link Payment} object
     * @param pendingTransactions A list of {@link PendingTransaction} objects
     * @param secondPassword      The double encryption password if necessary
     * @return An {@link Observable<String>}
     */
    public Observable<String> sendPayment(@NonNull Payment payment,
                                          @NonNull List<PendingTransaction> pendingTransactions,
                                          @Nullable String secondPassword) {
        return getPaymentObservable(payment, pendingTransactions, secondPassword)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<String> getPaymentObservable(Payment payment, List<PendingTransaction> pendingTransactions, String secondPassword) {
        throw new NotImplementedException("todo");
        // TODO: 22/02/2017  
//        return Observable.create(subscriber -> {
//            for (int i = 0; i < pendingTransactions.size(); i++) {
//                PendingTransaction pendingTransaction = pendingTransactions.get(i);
//
//                final int finalI = i;
//                try {
//
//                    LegacyAddress legacyAddress = ((LegacyAddress) pendingTransaction.sendingObject.accountObject);
//                    String changeAddress = legacyAddress.getAddress();
//                    String receivingAddress = payloadManager.getNextReceiveAddress(pendingTransaction.addressToReceiveIndex);
//
//                    List<ECKey> keys = new ArrayList<>();
//                    if (payloadManager.getPayload().isDoubleEncrypted()) {
//                        ECKey walletKey = legacyAddress.getECKey(secondPassword);
//                        keys.add(walletKey);
//                    } else {
//                        ECKey walletKey = legacyAddress.getECKey();
//                        keys.add(walletKey);
//                    }
//
//                    payment.submitPayment(
//                            pendingTransaction.unspentOutputBundle,
//                            keys,
//                            receivingAddress,
//                            changeAddress,
//                            pendingTransaction.bigIntFee,
//                            pendingTransaction.bigIntAmount,
//                            new Payment.SubmitPaymentListener() {
//                                @Override
//                                public void onSuccess(String s) {
//                                    if (!subscriber.isDisposed()) {
//                                        subscriber.onNext(s);
//                                    }
//
//                                    payloadManager.getPayload().getHdWallet().getAccounts().get(pendingTransaction.addressToReceiveIndex).incReceive();
//
//                                    long currentTotalBalance = multiAddrFactory.getLegacyBalance();
//                                    long currentAddressBalance = multiAddrFactory.getLegacyBalance(legacyAddress.getAddress());
//                                    long spentAmount = (pendingTransaction.bigIntAmount.longValue() + pendingTransaction.bigIntFee.longValue());
//
//                                    // Update Balances temporarily rather than wait for sync
//                                    multiAddrFactory.setLegacyBalance(currentTotalBalance - spentAmount);
//                                    multiAddrFactory.setLegacyBalance(
//                                            legacyAddress.getAddress(),
//                                            currentAddressBalance - spentAmount);
//
//                                    if (finalI == pendingTransactions.size() - 1) {
//                                        savePayloadToServer()
//                                                .blockingSubscribe(
//                                                        aBoolean -> {},
//                                                        Throwable::printStackTrace);
//                                        if (!subscriber.isDisposed()) {
//                                            subscriber.onComplete();
//                                        }
//                                    }
//                                }
//
//                                @Override
//                                public void onFail(String error) {
//                                    if (!subscriber.isDisposed()) {
//                                        subscriber.onError(new Throwable(error));
//                                    }
//                                }
//                            });
//                } catch (Exception e) {
//                    if (!subscriber.isDisposed()) {
//                        subscriber.onError(e);
//                    }
//                }
//            }
//        });
    }

    /**
     * Syncs the Wallet to the server, for instance after
     * archiving some addresses.
     *
     * @return boolean indicating success or not
     */
    public Observable<Boolean> savePayloadToServer() {
        return Observable.fromCallable(() -> payloadManager.save())
                .compose(RxUtil.applySchedulersToObservable());
    }
}
