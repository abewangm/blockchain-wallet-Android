package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.ui.transactions.PayloadDataManager;

public class AccountEditDataManager {

    private PayloadDataManager payloadDataManager;
    private PaymentService paymentService;
    private DynamicFeeCache dynamicFeeCache;

    public AccountEditDataManager(PaymentService paymentService,
        PayloadDataManager payloadDataManager,
        DynamicFeeCache dynamicFeeCache) {
        this.paymentService = paymentService;
        this.payloadDataManager = payloadDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
    }

    /**
     * Generates a {@link PendingTransaction} object for a given legacy address, where the output is
     * the default account in the user's wallet
     *
     * @param legacyAddress The {@link LegacyAddress} you wish to transfer funds from
     * @return An {@link Observable<PendingTransaction>}
     */
    public Observable<PendingTransaction> getPendingTransactionForLegacyAddress(LegacyAddress legacyAddress) {
        PendingTransaction pendingTransaction = new PendingTransaction();

        return paymentService.getUnspentOutputs(legacyAddress.getAddress())
            .flatMap(unspentOutputs -> {
                BigInteger suggestedFeePerKb =
                    new BigDecimal(dynamicFeeCache.getCachedDynamicFee()
                        .getDefaultFee()
                        .getFee())
                        .toBigInteger();

                Pair<BigInteger, BigInteger> sweepableCoins = paymentService
                    .getSweepableCoins(unspentOutputs, suggestedFeePerKb);
                BigInteger sweepAmount = sweepableCoins.getLeft();

                // To default account
                int defaultIndex = payloadDataManager.getDefaultAccountIndex();
                Account defaultAccount = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().get(defaultIndex);
                pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepAmount.toString(), "", sweepAmount.longValue(), legacyAddress);
                pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", sweepAmount.longValue(), defaultAccount);
                pendingTransaction.unspentOutputBundle = paymentService.getSpendableCoins(unspentOutputs, sweepAmount, suggestedFeePerKb);
                pendingTransaction.bigIntAmount = sweepAmount;
                pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

                return payloadDataManager.getNextReceiveAddress(defaultIndex);
            })
            .map(receivingAddress -> {
                pendingTransaction.receivingAddress = receivingAddress;
                return pendingTransaction;
            })
            .compose(RxUtil.applySchedulersToObservable());
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

}