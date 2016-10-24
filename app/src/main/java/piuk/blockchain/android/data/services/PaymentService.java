package piuk.blockchain.android.data.services;

import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;

import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class PaymentService {

    private Payment payment;

    public PaymentService(Payment payment) {
        this.payment = payment;
    }

    /**
     * Submits a payment to a specified address and returns the transaction hash if successful
     *
     * @param unspentOutputBundle UXTO object
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

        return Observable.create(subscriber -> {
            try {
                payment.submitPayment(
                        unspentOutputBundle,
                        keys,
                        toAddress,
                        changeAddress,
                        bigIntFee,
                        bigIntAmount,
                        new SubmitPaymentListener(subscriber));
            } catch (Exception e) {
                if (subscriber != null) {
                    subscriber.onError(new Throwable(e));
                }
            }
        });
    }

    private static class SubmitPaymentListener implements Payment.SubmitPaymentListener {

        private final Subscriber<? super String> subscriber;

        SubmitPaymentListener(Subscriber<? super String> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSuccess(String s) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(s);
                subscriber.onCompleted();
            }
        }

        @Override
        public void onFail(String s) {
            if (subscriber != null) {
                subscriber.onError(new Throwable(s));
            }
        }
    }
}
