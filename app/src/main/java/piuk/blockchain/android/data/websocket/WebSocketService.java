package piuk.blockchain.android.data.websocket;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;


public class WebSocketService extends Service {

    public static final String ACTION_INTENT = "info.blockchain.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";
    private final IBinder binder = new LocalBinder();
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected PrefsUtil prefsUtil;
    @Inject protected NotificationManager notificationManager;
    @Inject protected SwipeToReceiveHelper swipeToReceiveHelper;
    @Inject protected OkHttpClient okHttpClient;
    @Inject protected RxBus rxBus;
    @Thunk WebSocketHandler webSocketHandler;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_INTENT)) {
                if (intent.hasExtra("address") && webSocketHandler != null) {
                    webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
                }
                if (intent.hasExtra("xpub") && webSocketHandler != null) {
                    webSocketHandler.subscribeToXpub(intent.getStringExtra("xpub"));
                }
            }
        }
    };

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        String[] addrs = getAddresses();
        String[] xpubs = getXpubs();

        webSocketHandler = new WebSocketHandler(
                getApplicationContext(),
                okHttpClient,
                payloadDataManager,
                notificationManager,
                new EnvironmentSettings(),
                new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                prefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                xpubs,
                addrs,
                rxBus);

        webSocketHandler.start();
    }

    private String[] getXpubs() {
        int nbAccounts = 0;
        if (payloadDataManager.getWallet() != null) {
            if (payloadDataManager.getWallet().isUpgraded()) {
                try {
                    nbAccounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().size();
                } catch (java.lang.IndexOutOfBoundsException e) {
                    nbAccounts = 0;
                }
            }

            final String[] xpubs = new String[nbAccounts];
            for (int i = 0; i < nbAccounts; i++) {
                String s = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().get(i).getXpub();
                if (s != null && !s.isEmpty()) {
                    xpubs[i] = s;
                }
            }
            return xpubs;
        } else {
            return new String[0];
        }
    }

    private String[] getAddresses() {
        if (payloadDataManager.getWallet() != null) {
            int nbLegacy = payloadDataManager.getWallet().getLegacyAddressList().size();
            final String[] addrs = new String[nbLegacy];
            for (int i = 0; i < nbLegacy; i++) {
                String s = payloadDataManager.getWallet().getLegacyAddressList().get(i).getAddress();
                if (s != null && !s.isEmpty()) {
                    addrs[i] = payloadDataManager.getWallet().getLegacyAddressList().get(i).getAddress();
                }
            }

            return addrs;
        } else if (!swipeToReceiveHelper.getReceiveAddresses().isEmpty()) {
            final String[] addrs = new String[swipeToReceiveHelper.getReceiveAddresses().size()];
            final List<String> receiveAddresses = swipeToReceiveHelper.getReceiveAddresses();
            for (int i = 0; i < receiveAddresses.size(); i++) {
                final String address = receiveAddresses.get(i);
                addrs[i] = address;
            }
            return addrs;
        } else {
            return new String[0];
        }
    }

    @Override
    public void onDestroy() {
        if (webSocketHandler != null) webSocketHandler.stopPermanently();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private class LocalBinder extends Binder {

        LocalBinder() {
            // Empty constructor
        }

        @SuppressWarnings("unused") // Necessary for implementing bound Android Service
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
}
