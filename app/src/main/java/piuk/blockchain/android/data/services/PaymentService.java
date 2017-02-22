package piuk.blockchain.android.data.services;

import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import java.util.HashMap;
import okhttp3.ResponseBody;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.bitcoinj.core.Transaction;
import retrofit2.Call;
import retrofit2.Response;

public class PaymentService {

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

        return Observable.create(observableOnSubscribe -> {
            try {

                HashMap<String, BigInteger> receivers = new HashMap<String, BigInteger>();
                receivers.put(toAddress, bigIntAmount);

                Transaction tx = Payment.makeTransaction(
                    unspentOutputBundle.getSpendableOutputs(),
                    receivers,
                    bigIntFee,
                    changeAddress);

                Payment.signTransaction(tx, keys);

                // TODO: 17/02/2017 Improve this. I just got rid of errors here for now.
                Call<ResponseBody> call = Payment.publishTransaction(tx);

                Response<ResponseBody> exe = call.execute();

                SubmitPaymentListener listener = new SubmitPaymentListener(observableOnSubscribe);
                if(exe.isSuccessful()) {
                    listener.onSuccess(tx.getHashAsString());
                } else {
                    listener.onFail(exe.errorBody().string());
                }

            } catch (Exception e) {
                if (observableOnSubscribe != null && !observableOnSubscribe.isDisposed()) {
                    observableOnSubscribe.onError(e);
                }
            }
        });
    }

    private static class SubmitPaymentListener {

        private final ObservableEmitter<? super String> subscriber;

        SubmitPaymentListener(ObservableEmitter<String> subscriber) {
            this.subscriber = subscriber;
        }

        public void onSuccess(String s) {
            if (!subscriber.isDisposed()) {
                subscriber.onNext(s);
                subscriber.onComplete();
            }
        }

        public void onFail(String s) {
            if (!subscriber.isDisposed()) {
                subscriber.onError(new Throwable(s));
            }
        }
    }
}
