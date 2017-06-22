package piuk.blockchain.android.data.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

import piuk.blockchain.android.ui.balance.BalanceFragment;

class NetworkStateReceiver extends BroadcastReceiver {

    private static final long COOL_DOWN_INTERVAL = 1000 * 30L;
    private long lastBroadcastTime;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getExtras() != null) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                // Sends max of one broadcast every 30s if network connection is spotty
                if (System.currentTimeMillis() - lastBroadcastTime > COOL_DOWN_INTERVAL) {
                    LocalBroadcastManager.getInstance(context).sendBroadcastSync(new Intent(BalanceFragment.ACTION_INTENT));
                    lastBroadcastTime = System.currentTimeMillis();
                }
            }
        }
    }
}