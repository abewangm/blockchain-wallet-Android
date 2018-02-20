package piuk.blockchain.android.data.websocket;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;


public class WebSocketService extends Service {

    public static final String ACTION_INTENT = "piuk.blockchain.android.action.SUBSCRIBE_TO_ADDRESS";
    public static final String EXTRA_BITCOIN_ADDRESS = "piuk.blockchain.android.extras.EXTRA_BITCOIN_ADDRESS";
    public static final String EXTRA_X_PUB_BTC = "piuk.blockchain.android.extras.EXTRA_X_PUB_BTC";
    public static final String EXTRA_BITCOIN_CASH_ADDRESS = "piuk.blockchain.android.extras.EXTRA_BITCOIN_CASH_ADDRESS";
    public static final String EXTRA_X_PUB_BCH = "piuk.blockchain.android.extras.EXTRA_X_PUB_BCH";

    private final IBinder binder = new LocalBinder();
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected EthDataManager ethDataManager;
    @Inject protected BchDataManager bchDataManager;
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
                if (intent.hasExtra(EXTRA_BITCOIN_ADDRESS) && webSocketHandler != null) {
                    webSocketHandler.subscribeToAddressBtc(intent.getStringExtra(EXTRA_BITCOIN_ADDRESS));
                }
                if (intent.hasExtra(EXTRA_X_PUB_BTC) && webSocketHandler != null) {
                    webSocketHandler.subscribeToXpubBtc(intent.getStringExtra(EXTRA_X_PUB_BTC));
                }
                if (intent.hasExtra(EXTRA_BITCOIN_CASH_ADDRESS) && webSocketHandler != null) {
                    webSocketHandler.subscribeToAddressBch(intent.getStringExtra(EXTRA_BITCOIN_CASH_ADDRESS));
                }
                if (intent.hasExtra(EXTRA_X_PUB_BCH) && webSocketHandler != null) {
                    webSocketHandler.subscribeToXpubBch(intent.getStringExtra(EXTRA_X_PUB_BCH));
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

        int btcUnit = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        /*
         * Sanity check for odd issue on rooted devices. This is an odd bug to do with how Android
         * handles Shared Prefs + multiprocess apps, i.e. this Service runs in a different context
         * to Prefs and that somehow breaks Prefs if not configured with
         * setSharedPreferencesMode(Context.MODE_MULTI_PROCESS).
         */
        if (btcUnit > 2 || btcUnit < 0) btcUnit = 0;

        webSocketHandler = new WebSocketHandler(
                getApplicationContext(),
                okHttpClient,
                payloadDataManager,
                ethDataManager,
                bchDataManager,
                notificationManager,
                new EnvironmentSettings(),
                new MonetaryUtil(btcUnit),
                prefsUtil.getValue(PrefsUtil.KEY_GUID, ""),
                getXpubsBtc(),
                getAddressesBtc(),
                getXpubsBch(),
                getAddressesBch(),
                getEthAccount(),
                rxBus);

        webSocketHandler.start();
    }

    private String[] getXpubsBtc() {
        int nbAccounts = 0;
        if (payloadDataManager.getWallet() != null) {
            if (payloadDataManager.getWallet().isUpgraded()) {
                try {
                    nbAccounts = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().size();
                } catch (IndexOutOfBoundsException e) {
                    nbAccounts = 0;
                }
            }

            final String[] xpubs = new String[nbAccounts];
            for (int i = 0; i < nbAccounts; i++) {
                String xPub = payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().get(i).getXpub();
                if (xPub != null && !xPub.isEmpty()) {
                    xpubs[i] = xPub;
                }
            }
            return xpubs;
        } else {
            return new String[0];
        }
    }

    private String[] getAddressesBtc() {
        if (payloadDataManager.getWallet() != null) {
            int nbLegacy = payloadDataManager.getWallet().getLegacyAddressList().size();
            final String[] addrs = new String[nbLegacy];
            for (int i = 0; i < nbLegacy; i++) {
                String address = payloadDataManager.getWallet().getLegacyAddressList().get(i).getAddress();
                if (address != null && !address.isEmpty()) {
                    addrs[i] = address;
                }
            }

            return addrs;
        } else if (!swipeToReceiveHelper.getBitcoinReceiveAddresses().isEmpty()) {
            final String[] addrs = new String[swipeToReceiveHelper.getBitcoinReceiveAddresses().size()];
            final List<String> receiveAddresses = swipeToReceiveHelper.getBitcoinReceiveAddresses();
            for (int i = 0; i < receiveAddresses.size(); i++) {
                final String address = receiveAddresses.get(i);
                addrs[i] = address;
            }
            return addrs;
        } else {
            return new String[0];
        }
    }

    private String[] getXpubsBch() {
        int nbAccounts = 0;
        if (payloadDataManager.getWallet() != null) {
            if (payloadDataManager.getWallet().isUpgraded()) {
                nbAccounts = bchDataManager.getActiveXpubs().size();
            }

            final String[] xpubs = new String[nbAccounts];
            for (int i = 0; i < nbAccounts; i++) {
                String xPub = bchDataManager.getActiveXpubs().get(i);
                if (xPub != null && !xPub.isEmpty()) {
                    xpubs[i] = xPub;
                }
            }
            return xpubs;
        } else {
            return new String[0];
        }
    }

    private String[] getAddressesBch() {
        if (payloadDataManager.getWallet() != null) {
            int nbLegacy = bchDataManager.getLegacyAddressStringList().size();
            final String[] addrs = new String[nbLegacy];
            for (int i = 0; i < nbLegacy; i++) {
                String address = bchDataManager.getLegacyAddressStringList().get(i);
                if (address != null && !address.isEmpty()) {
                    addrs[i] = address;
                }
            }

            return addrs;
        } else if (!swipeToReceiveHelper.getBitcoinCashReceiveAddresses().isEmpty()) {
            final String[] addrs = new String[swipeToReceiveHelper.getBitcoinCashReceiveAddresses().size()];
            final List<String> receiveAddresses = swipeToReceiveHelper.getBitcoinCashReceiveAddresses();
            for (int i = 0; i < receiveAddresses.size(); i++) {
                final String address = receiveAddresses.get(i);
                addrs[i] = address;
            }
            return addrs;
        } else {
            return new String[0];
        }
    }

    @Nullable
    private String getEthAccount() {
        if (ethDataManager.getEthWallet() != null && ethDataManager.getEthWallet().getAccount() != null) {
            return ethDataManager.getEthWallet().getAccount().getAddress();
        } else if (swipeToReceiveHelper.getEthReceiveAddress() != null) {
            return swipeToReceiveHelper.getEthReceiveAddress();
        }
        return null;
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
