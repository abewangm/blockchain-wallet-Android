package piuk.blockchain.android.data.services;

import info.blockchain.wallet.payment.Payment;

import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;

public class UnspentService {

    // TODO: 22/02/2017  
//    /**
//     * Get an {@link UnspentOutputs} object for a given legacy address as a string
//     *
//     * @param legacyAddress The address from which to calculate the unspent outputs
//     * @param payment       A new {@link Payment} object
//     * @return {@link Observable<UnspentOutputs>}
//     */
//    public Observable<UnspentOutputs> getUnspentOutputs(String legacyAddress, Payment payment) {
//        return Observable.fromCallable(() -> unspent.getUnspentOutputs(legacyAddress))
//                .doOnNext(jsonObject -> {
//                    if (jsonObject == null) throw Exceptions.propagate(new Throwable("Response was null"));
//                })
//                .flatMap(jsonObject -> Observable.just(payment.getCoins(jsonObject)));
//    }
}
