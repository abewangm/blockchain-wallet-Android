package piuk.blockchain.android.data.connectivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.support.v4.content.LocalBroadcastManager;

import piuk.blockchain.android.data.rxjava.IgnorableSubscriber;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import rx.Completable;
import rx.android.schedulers.AndroidSchedulers;

public class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {

    private final NetworkRequest networkRequest;
    private Context context;

    public ConnectionStateMonitor(Context context) {
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
        broadcastOnMainThread().subscribe(new IgnorableSubscriber<>());
    }

    private Completable broadcastOnMainThread() {
        return Completable.fromAction(() ->
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcastSync(new Intent(BalanceFragment.ACTION_INTENT)))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

}
