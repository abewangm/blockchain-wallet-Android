package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;

public class AccountEditDataManager {

    private PayloadDataManager payloadDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private SendDataManager sendDataManager;

    public AccountEditDataManager(PayloadDataManager payloadDataManager,
                                  SendDataManager sendDataManager,
                                  DynamicFeeCache dynamicFeeCache) {
        this.payloadDataManager = payloadDataManager;
        this.sendDataManager = sendDataManager;
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

        return sendDataManager.getUnspentOutputs(legacyAddress.getAddress())
                .flatMap(unspentOutputs -> {
                    BigInteger suggestedFeePerKb =
                            BigInteger.valueOf(dynamicFeeCache.getFeeOptions().getRegularFee() * 1000);

                    Pair<BigInteger, BigInteger> sweepableCoins =
                            sendDataManager.getSweepableCoins(unspentOutputs, suggestedFeePerKb);
                    BigInteger sweepAmount = sweepableCoins.getLeft();

                    // To default account
                    Account defaultAccount = payloadDataManager.getDefaultAccount();
                    pendingTransaction.sendingObject = new ItemAccount(legacyAddress.getLabel(), sweepAmount.toString(), "", sweepAmount.longValue(), legacyAddress);
                    pendingTransaction.receivingObject = new ItemAccount(defaultAccount.getLabel(), "", "", sweepAmount.longValue(), defaultAccount);
                    pendingTransaction.unspentOutputBundle = sendDataManager.getSpendableCoins(unspentOutputs, sweepAmount, suggestedFeePerKb);
                    pendingTransaction.bigIntAmount = sweepAmount;
                    pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle.getAbsoluteFee();

                    return payloadDataManager.getNextReceiveAddress(defaultAccount);
                })
                .map(receivingAddress -> {
                    pendingTransaction.receivingAddress = receivingAddress;
                    return pendingTransaction;
                });
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

        return sendDataManager.submitPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount);
    }

}