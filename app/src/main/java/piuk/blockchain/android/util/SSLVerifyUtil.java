package piuk.blockchain.android.util;

import java.io.IOException;

import javax.net.ssl.SSLPeerUnverifiedException;

import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.api.ConnectionApi;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

// openssl s_client -showcerts -connect blockchain.info:443

public class SSLVerifyUtil {

    private final RxPinning rxPinning;
    private ConnectionApi connectionApi;

    public SSLVerifyUtil(RxBus rxBus, ConnectionApi connectionApi) {
        this.connectionApi = connectionApi;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Pings the website to check for a connection. If the call returns an {@link
     * IOException} or {@link SSLPeerUnverifiedException}, the {@link
     * RxPinning} object will broadcast this to the {@link BaseAuthActivity}
     * which will handle the response appropriately.
     */
    public void validateSSL() {
        rxPinning.callObservable(() -> connectionApi.getWebiteConnection())
                .subscribeOn(Schedulers.io())
                .subscribe(new IgnorableDefaultObserver<>());
    }

}