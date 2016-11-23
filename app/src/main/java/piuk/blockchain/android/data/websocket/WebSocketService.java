package piuk.blockchain.android.data.websocket;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.annotations.Thunk;

public class WebSocketService extends Service {

    private static final String TAG = WebSocketService.class.getSimpleName();

    public static final String ACTION_INTENT = "info.blockchain.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";
    private final IBinder binder = new LocalBinder();
    @Inject protected PayloadManager payloadManager;
    @Thunk WebSocketHandler webSocketHandler;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (ACTION_INTENT.equals(intent.getAction())) {
                webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
                webSocketHandler.subscribeToXpub(intent.getStringExtra("xpub"));
            }
        }
    };

    {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        String[] addrs = getAddresses();
        String[] xpubs = getXpubs();

        if (addrs.length > 0 || xpubs.length > 0) {
            webSocketHandler = new WebSocketHandler(getApplicationContext(), payloadManager.getPayload().getGuid(), xpubs, addrs);
            webSocketHandler.start();
        }
    }

    private String[] getXpubs() {
        int nbAccounts = 0;
        if (payloadManager.getPayload().isUpgraded()) {
            try {
                nbAccounts = payloadManager.getPayload().getHdWallet().getAccounts().size();
            } catch (java.lang.IndexOutOfBoundsException e) {
                nbAccounts = 0;
            }
        }

        final String[] xpubs = new String[nbAccounts];
        for (int i = 0; i < nbAccounts; i++) {
            String s = payloadManager.getPayload().getHdWallet().getAccounts().get(i).getXpub();
            if (s != null && s.length() > 0) {
                xpubs[i] = s;
            }
        }

        return xpubs;
    }

    private String[] getAddresses() {
        int nbLegacy = payloadManager.getPayload().getLegacyAddressList().size();
        final String[] addrs = new String[nbLegacy];
        for (int i = 0; i < nbLegacy; i++) {
            String s = payloadManager.getPayload().getLegacyAddressList().get(i).getAddress();
            if (s != null && s.length() > 0) {
                addrs[i] = payloadManager.getPayload().getLegacyAddressList().get(i).getAddress();
            }
        }

        return addrs;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        if (webSocketHandler != null) webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private class LocalBinder extends Binder {
        @SuppressWarnings("unused") // Necessary for implementing bound Android Service
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
}