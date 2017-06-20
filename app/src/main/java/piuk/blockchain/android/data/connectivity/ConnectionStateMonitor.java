package piuk.blockchain.android.data.connectivity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.ui.balance.LegacyBalanceFragment;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {

    private static final long COOL_DOWN_INTERVAL = 1000 * 30L;
    private final NetworkRequest networkRequest;
    private long lastBroadcastTime;
    private Context context;

    ConnectionStateMonitor(Context context) {
        this.context = context;
        networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    public void enable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        connectivityManager.registerNetworkCallback(networkRequest, this);
    }

    @Override
    public void onAvailable(Network network) {
        // Sends max of one broadcast every 30s if network connection is spotty
        if (System.currentTimeMillis() - lastBroadcastTime > COOL_DOWN_INTERVAL) {
            broadcastOnMainThread().subscribe(new IgnorableDefaultObserver<>());
            lastBroadcastTime = System.currentTimeMillis();
        }
    }

    private Completable broadcastOnMainThread() {
        return Completable.fromAction(() ->
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcastSync(new Intent(LegacyBalanceFragment.ACTION_INTENT)))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

}
