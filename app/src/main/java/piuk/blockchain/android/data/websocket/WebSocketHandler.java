package piuk.blockchain.android.data.websocket;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import info.blockchain.wallet.payload.PayloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Completable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.NotificationsUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;


@SuppressWarnings("WeakerAccess")
class WebSocketHandler {

    @Thunk static final String TAG = WebSocketHandler.class.getSimpleName();

    private final static long PING_INTERVAL = 20000L; // Ping every 20 seconds
    private final static long PONG_TIMEOUT = 5000L; // Pong timeout after 5 seconds

    private String[] xpubs;
    private String[] addrs;
    private Timer pingTimer;
    boolean pingPongSuccess = false;
    @Thunk String guid;
    @Thunk WebSocket connection;
    @Thunk HashSet<String> subHashSet = new HashSet<>();
    @Thunk HashSet<String> onChangeHashSet = new HashSet<>();
    @Thunk MonetaryUtil monetaryUtil;
    @Thunk PayloadManager payloadManager;
    @Thunk Context context;

    public WebSocketHandler(Context context, String guid, String[] xpubs, String[] addrs) {
        this.context = context;
        this.guid = guid;
        this.xpubs = xpubs;
        this.addrs = addrs;
        payloadManager = PayloadManager.getInstance();
        final PrefsUtil prefsUtil = new PrefsUtil(context);
        monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public void send(String message) {
        // Make sure each message is only sent once per socket lifetime
        if (!subHashSet.contains(message)) {
            try {
                if (connection != null && connection.isOpen()) {
                    connection.sendText(message);
                    subHashSet.add(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "send: ", e);
            }
        }
    }

    public synchronized void subscribe() {
        if (guid == null) {
            return;
        }
        send("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

        for (String xpub : xpubs) {
            if (xpub != null && xpub.length() > 0) {
                send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
            }
        }

        for (String addr : addrs) {
            if (addr != null && addr.length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\"" + addr + "\"}");
            }
        }
    }

    public synchronized void subscribeToXpub(String xpub) {
        if (xpub != null && !xpub.isEmpty()) {
            send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
        }
    }

    public synchronized void subscribeToAddress(String address) {
        if (address != null && !address.isEmpty())
            send("{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}");
    }

    public void stop() {
        Log.d(TAG, "stop: ");
        stopPingTimer();

        if (connection != null && connection.isOpen()) {
            connection.disconnect();
        }
    }

    public void start() {
        stop();
        connectToWebSocket().subscribe(new IgnorableDefaultObserver<>());
        startPingTimer();
    }

    private void startPingTimer() {
        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connection != null) {
                    if (connection.isOpen()) {
                        pingPongSuccess = false;
                        connection.sendPing();
                        startPongTimer();
                        Log.d(TAG, "run: sendPing");
                    } else {
                        start();
                        Log.d(TAG, "run: start");
                    }
                }
            }
        }, PING_INTERVAL, PING_INTERVAL);
    }

    private void stopPingTimer() {
        if (pingTimer != null) pingTimer.cancel();
    }

    @Thunk
    void startPongTimer() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!pingPongSuccess) {
                    // Ping pong unsuccessful after x seconds - restart connection
                    start();
                }
            }
        }, PONG_TIMEOUT);
    }

    @Thunk
    void updateBalancesAndTransactions() {
        Log.d(TAG, "updateBalancesAndTransactions: ");
        updateBalancesAndTxs().subscribe(new IgnorableDefaultObserver<>());
    }

    private Completable updateBalancesAndTxs() {
        return Completable.fromCallable(() -> {
            payloadManager.updateBalancesAndTransactions();
            return Void.TYPE;
        }).doOnComplete(() -> {
            Log.d(TAG, "updateBalancesAndTxs: send broadcast");
            Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private Completable connectToWebSocket() {
        return Completable.fromCallable(() -> {
            // Seems we make a new connection here, so we should clear our HashSet
            subHashSet.clear();
            Log.d(TAG, "doInBackground: started");

            connection = new WebSocketFactory()
                    .createSocket("wss://ws.blockchain.info/inv")
                    .addHeader("Origin", "https://blockchain.info").recreate()
                    .addListener(new WebSocketAdapter() {
                        @Override
                        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                            super.onPongFrame(websocket, frame);
                            pingPongSuccess = true;
                            Log.d(TAG, "onPongFrame: ");
                        }

                        @Override
                        public void onTextMessage(WebSocket websocket, String message) {
                            Log.d(TAG, "onTextMessage: ");
                            if (guid == null) {
                                return;
                            }

                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(message);
                                attemptParseMessage(message, jsonObject);
                            } catch (JSONException je) {
                                Log.e(TAG, "onTextMessage: ", je);
                            }

                        }
                    });
            connection.connect();
            subscribe();

            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    @Thunk
    void attemptParseMessage(String message, JSONObject jsonObject) {
        try {
            String op = (String) jsonObject.get("op");
            if (op.equals("utx") && jsonObject.has("x")) {

                JSONObject objX = (JSONObject) jsonObject.get("x");

                long value = 0L;
                long totalValue = 0L;
                String inAddr = null;

                if (objX.has("inputs")) {
                    JSONArray inputArray = (JSONArray) objX.get("inputs");
                    JSONObject inputObj;
                    for (int j = 0; j < inputArray.length(); j++) {
                        inputObj = (JSONObject) inputArray.get(j);
                        if (inputObj.has("prev_out")) {
                            JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                            if (prevOutObj.has("value")) {
                                value = prevOutObj.getLong("value");
                            }
                            if (prevOutObj.has("xpub")) {
                                totalValue -= value;
                            } else if (prevOutObj.has("addr")) {
                                if (payloadManager.getPayload().containsLegacyAddress((String) prevOutObj.get("addr"))) {
                                    totalValue -= value;
                                } else if (inAddr == null) {
                                    inAddr = (String) prevOutObj.get("addr");
                                }
                            }
                        }
                    }
                }

                if (objX.has("out")) {
                    JSONArray outArray = (JSONArray) objX.get("out");
                    JSONObject outObj;
                    for (int j = 0; j < outArray.length(); j++) {
                        outObj = (JSONObject) outArray.get(j);
                        if (outObj.has("value")) {
                            value = outObj.getLong("value");
                        }
                        if (outObj.has("xpub")) {
                            totalValue += value;
                        } else if (outObj.has("addr")) {
                            if (payloadManager.getPayload().containsLegacyAddress((String) outObj.get("addr"))) {
                                totalValue += value;
                            }
                        }
                    }
                }

                String title = context.getString(R.string.app_name);
                if (totalValue > 0L) {
                    String marquee = context.getString(R.string.received_bitcoin) + " " + monetaryUtil.getBTCFormat().format((double) totalValue / 1e8) + " BTC";
                    String text = marquee;
                    if (totalValue > 0) {
                        text += " from " + inAddr;
                    }

                    triggerNotification(title, marquee, text);
                }

                updateBalancesAndTransactions();

            } else if (op.equals("on_change")) {
                final String localChecksum = payloadManager.getCheckSum();

                boolean isSameChecksum = false;
                if (jsonObject.has("checksum")) {
                    final String remoteChecksum = (String) jsonObject.get("checksum");
                    isSameChecksum = remoteChecksum.equals(localChecksum);
                }

                if (!onChangeHashSet.contains(message) && !isSameChecksum) {
                    // Remote update to wallet data detected
                    if (payloadManager.getTempPassword() != null) {
                        // Download changed payload
                        payloadManager.initiatePayload(payloadManager.getPayload().getSharedKey(),
                                payloadManager.getPayload().getGuid(),
                                payloadManager.getTempPassword(), () -> {
                                    // No-op
                                });
                        ToastCustom.makeText(context, context.getString(R.string.wallet_updated), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                        updateBalancesAndTransactions();
                    }

                    onChangeHashSet.add(message);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "attemptParseMessage: ", e);
        }
    }

    private void triggerNotification(String title, String marquee, String text) {
        new NotificationsUtil(context).setNotification(
                title,
                marquee,
                text,
                R.drawable.ic_notification_transparent,
                R.drawable.ic_launcher,
                MainActivity.class,
                1000);
    }
}
