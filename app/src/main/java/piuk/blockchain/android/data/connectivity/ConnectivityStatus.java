package piuk.blockchain.android.data.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityStatus {

    ConnectivityStatus() {
        // No-op
    }

    public static boolean hasConnectivity(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                NetworkInfo info = manager.getActiveNetworkInfo();
                if (info != null && info.isConnectedOrConnecting()) {
                    return true;
                }
            }

        }
        return false;
    }

}
