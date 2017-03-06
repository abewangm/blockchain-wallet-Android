package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.api.data.FeeList;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import io.reactivex.Completable;
import java.io.IOException;
import java.util.Arrays;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.payload.PayloadBridge;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.PaymentService;
import retrofit2.Response;

public class SendDataManager {

    private PaymentService paymentService;

    public SendDataManager(PaymentService paymentService) {
        this.paymentService = paymentService;
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

    public Observable<ECKey> getEcKeyFromBip38(String password, String scanData, NetworkParameters networkParameters) {
        return Observable.fromCallable(() -> {
            BIP38PrivateKey bip38 = new BIP38PrivateKey(networkParameters, scanData);
            return bip38.decrypt(password);
        }).compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<FeeList> getSuggestedFee() {
        return Observable.fromCallable(() -> {

            Response<FeeList> call = Payment
                .getDynamicFee().execute();

            if(call.isSuccessful()) {
                return call.body();
            } else {
                throw new Exception("Dynamic fee api call failed.");
            }
        })
        .compose(RxUtil.applySchedulersToObservable());
    }

    public Observable<UnspentOutputs> getUnspentOutputs(String address) {
        return Observable.fromCallable(() -> {
            Response<UnspentOutputs> call = Payment.getUnspentCoins(Arrays.asList(address))
                .execute();

            if(call.isSuccessful()) {
                return call.body();
            } else if(call.errorBody().string().equals("No free outputs to spend")) {
                //If no unspent outputs available server responds with 500?
                return UnspentOutputs.fromJson("{\"unspent_outputs\":[]}");
            } else {
                throw new Exception("Unspent api call failed.");
            }
        })
        .compose(RxUtil.applySchedulersToObservable());
    }

}
