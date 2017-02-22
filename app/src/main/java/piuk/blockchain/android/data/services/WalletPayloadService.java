package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.WalletApi;
import org.apache.commons.lang3.NotImplementedException;
import piuk.blockchain.android.data.rxjava.RxUtil;
import io.reactivex.Observable;

public class WalletPayloadService {

    /**
     * Get encrypted copy of Payload
     *
     * @param guid          A user's GUID
//     * @param sessionId     The session ID
     * @return              {@link Observable<String>} wrapping an encrypted Payload
     */
    // TODO: 21/02/2017 removed sessionId - might not need it anymore
    public Observable<String> getEncryptedPayload(String guid) {
        // TODO: 21/02/2017
        throw new NotImplementedException("");
//        return Observable.fromCallable(() -> WalletApi.fetchEncryptedPayload(guid))
//                .compose(RxUtil.applySchedulersToObservable());
    }

//    /**
//     * Get the current session ID
//     *
//     * @param guid          A user's GUID
//     * @return              {@link Observable<String>} wrapping the session ID
//     */
    // TODO: 22/02/2017  
//    public Observable<String> getSessionId(String guid) {
//        return Observable.fromCallable(() -> walletPayload.getSessionId(guid))
//                .compose(RxUtil.applySchedulersToObservable());
//    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid          A user's GUID
     * @return              {@link Observable<String>} wrapping the pairing encryption password
     */
    public Observable<String> getPairingEncryptionPassword(String guid) {
        // TODO: 21/02/2017
        throw new NotImplementedException("");
//        return Observable.fromCallable(() -> WalletApi.fetchPairingEncryptionPassword(guid))
//                .compose(RxUtil.applySchedulersToObservable());
    }
}
