package piuk.blockchain.android.data.services;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import piuk.blockchain.android.util.annotations.WebRequest;
import retrofit2.Response;

public class PaymentService {

    private Payment payment;

    public PaymentService(Payment payment) {
        this.payment = payment;
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
    @WebRequest
    public Observable<String> submitPayment(SpendableUnspentOutputs unspentOutputBundle,
                                            List<ECKey> keys,
                                            String toAddress,
                                            String changeAddress,
                                            BigInteger bigIntFee,
                                            BigInteger bigIntAmount) {

        return Observable.create(observableOnSubscribe -> {
            HashMap<String, BigInteger> receivers = new HashMap<>();
            receivers.put(toAddress, bigIntAmount);

            Transaction tx = payment.makeTransaction(
                    unspentOutputBundle.getSpendableOutputs(),
                    receivers,
                    bigIntFee,
                    changeAddress);

            payment.signTransaction(tx, keys);

            Response<ResponseBody> exe = payment.publishTransaction(tx).execute();

            if (exe.isSuccessful()) {
                if (!observableOnSubscribe.isDisposed()) {
                    observableOnSubscribe.onNext(tx.getHashAsString());
                    observableOnSubscribe.onComplete();
                }
            } else {
                if (!observableOnSubscribe.isDisposed()) {
                    observableOnSubscribe.onError(new Throwable(exe.code() + ": " + exe.errorBody().string()));
                }
            }
        });
    }

    /**
     * Returns an {@link UnspentOutputs} object containing all the unspent outputs for a given
     * address.
     *
     * @param address The address you wish to query, as a String
     * @return An {@link Observable<UnspentOutputs>}
     */
    @WebRequest
    public Observable<UnspentOutputs> getUnspentOutputs(String address) {
        return Observable.fromCallable(() -> {
            Response<UnspentOutputs> response = payment.getUnspentCoins(Collections.singletonList(address)).execute();

            if (response.isSuccessful()) {
                return response.body();
            } else if (response.code() == 500) {
                // If no unspent outputs available server responds with 500
                return UnspentOutputs.fromJson("{\"unspent_outputs\":[]}");
            } else {
                throw new ApiException(String.valueOf(response.code()));
            }
        });
    }

    /**
     * Returns a {@link SpendableUnspentOutputs} object from a given {@link UnspentOutputs} object,
     * given the payment amount and the current fee per kB. This method selects the minimum number
     * of inputs necessary to allow a successful payment by selecting from the largest inputs
     * first.
     *
     * @param unspentCoins  The addresses' {@link UnspentOutputs}
     * @param paymentAmount The amount you wish to send, as a {@link BigInteger}
     * @param feePerKb      The current fee per kB, as a {@link BigInteger}
     * @return An {@link SpendableUnspentOutputs} object, which wraps a list of spendable outputs
     * for the given inputs
     */
    public SpendableUnspentOutputs getSpendableCoins(UnspentOutputs unspentCoins,
                                                     BigInteger paymentAmount,
                                                     BigInteger feePerKb) throws UnsupportedEncodingException {
        return payment.getSpendableCoins(unspentCoins, paymentAmount, feePerKb);
    }

    /**
     * Calculates the total amount of bitcoin that can be swept from an {@link UnspentOutputs}
     * object and returns the amount that can be recovered, along with the fee (in absolute terms)
     * necessary to sweep those coins.
     *
     * @param unspentCoins An {@link UnspentOutputs} object that you wish to sweep
     * @param feePerKb     The current fee per kB on the network
     * @return A {@link Pair} object, where left = the sweepable amount as a {@link BigInteger},
     * right = the absolute fee needed to sweep those coins, also as a {@link BigInteger}
     */
    public Pair<BigInteger, BigInteger> getSweepableCoins(UnspentOutputs unspentCoins,
                                                          BigInteger feePerKb) {
        return payment.getSweepableCoins(unspentCoins, feePerKb);
    }

    /**
     * Returns true if the {@code absoluteFee} is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs      The number of inputs
     * @param outputs     The number of outputs
     * @param absoluteFee The absolute fee as a {@link BigInteger}
     * @return True if the fee is adequate, false if not
     */
    public boolean isAdequateFee(int inputs, int outputs, BigInteger absoluteFee) {
        return payment.isAdequateFee(inputs, outputs, absoluteFee);
    }

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs  The number of inputs
     * @param outputs The number of outputs
     * @return The estimated size of the transaction in kB
     */
    public int estimateSize(int inputs, int outputs) {
        return payment.estimatedSize(inputs, outputs);
    }

    /**
     * Returns an estimated absolute fee in satoshis (as a {@link BigInteger} for a given number of
     * inputs and outputs.
     *
     * @param inputs   The number of inputs
     * @param outputs  The number of outputs
     * @param feePerKb The current fee per kB om the network
     * @return A {@link BigInteger} representing the absolute fee
     */
    public BigInteger estimateFee(int inputs, int outputs, BigInteger feePerKb) {
        return payment.estimatedFee(inputs, outputs, feePerKb);
    }

}
