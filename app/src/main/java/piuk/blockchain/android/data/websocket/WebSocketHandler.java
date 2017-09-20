package piuk.blockchain.android.data.websocket;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.utils.Convert;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.ethereum.models.CombinedEthModel;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.websocket.models.EthWebsocketResponse;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.NotificationsUtil;
import timber.log.Timber;


@SuppressWarnings("WeakerAccess")
class WebSocketHandler extends WebSocketListener {

    private final static long RETRY_INTERVAL = 5 * 1000L;
    /**
     * Websocket status code as defined by <a href="http://tools.ietf.org/html/rfc6455#section-7.4">Section
     * 7.4 of RFC 6455</a>
     */
    private static final int STATUS_CODE_NORMAL_CLOSURE = 1000;

    private boolean stoppedDeliberately = false;
    private String[] xpubs;
    private String[] addrs;
    private String ethAccount;
    private EthDataManager ethDataManager;
    private NotificationManager notificationManager;
    private String guid;
    private HashSet<String> subHashSet = new HashSet<>();
    private HashSet<String> onChangeHashSet = new HashSet<>();
    private EnvironmentSettings environmentSettings;
    private MonetaryUtil monetaryUtil;
    private Context context;
    private OkHttpClient okHttpClient;
    private WebSocket btcConnection, ethConnection;
    private boolean connected;
    private PayloadDataManager payloadDataManager;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private RxBus rxBus;

    public WebSocketHandler(Context context,
                            OkHttpClient okHttpClient,
                            PayloadDataManager payloadDataManager,
                            EthDataManager ethDataManager,
                            NotificationManager notificationManager,
                            EnvironmentSettings environmentSettings,
                            MonetaryUtil monetaryUtil,
                            String guid,
                            String[] xpubs,
                            String[] addrs,
                            String ethAccount,
                            RxBus rxBus) {

        this.context = context;
        this.okHttpClient = okHttpClient;
        this.payloadDataManager = payloadDataManager;
        this.ethDataManager = ethDataManager;
        this.notificationManager = notificationManager;
        this.environmentSettings = environmentSettings;
        this.monetaryUtil = monetaryUtil;
        this.guid = guid;
        this.xpubs = xpubs;
        this.addrs = addrs;
        this.ethAccount = ethAccount;
        this.rxBus = rxBus;
    }

    public void subscribeToXpub(String xpub) {
        if (xpub != null && !xpub.isEmpty()) {
            sendToBtcConnection("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
        }
    }

    public void subscribeToAddress(String address) {
        if (address != null && !address.isEmpty()) {
            sendToBtcConnection("{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}");
        }
    }

    private void subscribeToEthAccount(String ethAddress) {
        if (ethAddress != null && !ethAddress.isEmpty()) {
            sendToEthConnection("{\"op\":\"account_sub\", \"account\":\"" + ethAddress + "\"}");
        }
    }

    /**
     * Starts listening for updates to subscribed xpubs and addresses. Will attempt reconnection
     * every 5 seconds if it cannot connect immediately.
     */
    public void start() {
        stop();
        stoppedDeliberately = false;
        connectToWebSocket()
                .doOnError(throwable -> attemptReconnection())
                .subscribe(new IgnorableDefaultObserver<>());
    }

    /**
     * Halts and disconnects the WebSocket service whilst preventing reconnection until {@link
     * #start()} is called
     */
    public void stopPermanently() {
        stoppedDeliberately = true;
        stop();
    }

    private void stop() {
        if (isConnected()) {
            btcConnection.close(STATUS_CODE_NORMAL_CLOSURE, "Websocket deliberately stopped");
            ethConnection.close(STATUS_CODE_NORMAL_CLOSURE, "Websocket deliberately stopped");
            btcConnection = null;
            ethConnection = null;
        }
    }

    private void sendToBtcConnection(String message) {
        // Make sure each message is only sent once per socket lifetime
        if (!subHashSet.contains(message)) {
            try {
                if (isConnected()) {
                    btcConnection.send(message);
                    subHashSet.add(message);
                }
            } catch (Exception e) {
                Timber.e(e, "Send to BTC websocket failed");
            }
        }
    }

    private void sendToEthConnection(String message) {
        // Make sure each message is only sent once per socket lifetime
        if (!subHashSet.contains(message)) {
            try {
                if (isConnected()) {
                    ethConnection.send(message);
                    subHashSet.add(message);
                }
            } catch (Exception e) {
                Timber.e(e, "Send to ETH websocket failed");
            }
        }
    }

    private void subscribe() {
        if (guid == null) {
            return;
        }
        sendToBtcConnection("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

        for (String xpub : xpubs) {
            subscribeToXpub(xpub);
        }

        for (String addr : addrs) {
            subscribeToAddress(addr);
        }

        subscribeToEthAccount(ethAccount);
    }

    private void attemptReconnection() {
        if (compositeDisposable.size() == 0 && !stoppedDeliberately) {
            compositeDisposable.add(
                    getReconnectionObservable()
                            .subscribe(
                                    value -> Timber.d("attemptReconnection: " + value),
                                    throwable -> Timber.e(throwable, "Attempt reconnection failed")));
        }
    }

    private Observable<Long> getReconnectionObservable() {
        return Observable.interval(RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .takeUntil((ObservableSource<Object>) aLong -> isConnected())
                .doOnNext(tick -> start());
    }

    private boolean isConnected() {
        return btcConnection != null && ethConnection != null && connected;
    }

    private void updateBalancesAndTransactions() {
        payloadDataManager.updateAllBalances()
                .andThen(payloadDataManager.updateAllTransactions())
                .doOnComplete(this::sendBroadcast)
                .subscribe(new IgnorableDefaultObserver<>());
    }

    private void sendBroadcast() {
        Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void startWebSocket() {
        Request btcRequest = new Request.Builder()
                .url(environmentSettings.getBtcWebsocketUrl())
                .addHeader("Origin", "https://blockchain.info")
                .build();

        Request ethRequest = new Request.Builder()
                .url(environmentSettings.getEthWebsocketUrl())
                .addHeader("Origin", "https://blockchain.info")
                .build();

        btcConnection = okHttpClient.newWebSocket(btcRequest, this);
        ethConnection = okHttpClient.newWebSocket(ethRequest, this);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        connected = true;
        compositeDisposable.clear();
        subscribe();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        if (payloadDataManager.getWallet() != null) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(text);
                attemptParseMessage(text, jsonObject);
            } catch (JSONException je) {
                Timber.e(je);
            }
        } else if (text.contains("account_sub")) {
            attemptParseEthMessage(text);
        } else {
            // Ignore content and broadcast anyway so that SwipeToReceive can update
            sendBroadcast();
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        super.onClosed(webSocket, code, reason);
        connected = false;
        attemptReconnection();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        connected = false;
        attemptReconnection();
    }

    private Completable connectToWebSocket() {
        return Completable.fromCallable(() -> {
            subHashSet.clear();
            startWebSocket();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private void attemptParseEthMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new KotlinModule());
            EthWebsocketResponse response = mapper.readValue(message, EthWebsocketResponse.class);

            String from = response.getTx().getFrom();
            // Check if money was received or sent
            if (ethAccount != null && !ethAccount.equals(from)) {
                String title = context.getString(R.string.app_name);
                String marquee = context.getString(R.string.received_ethereum)
                        + " "
                        + Convert.fromWei(response.getTx().getValue(), Convert.Unit.ETHER)
                        + " ETH";

                String text = marquee
                        + " "
                        + context.getString(R.string.from).toLowerCase() + " " + response.getTx().getFrom();

                triggerNotification(title, marquee, text);
            }

            if (ethDataManager.getEthWallet() != null) {
                downloadEthTransactions()
                        .subscribe(
                                combinedEthModel -> sendBroadcast(),
                                throwable -> Timber.e(throwable, "downloadEthTransactions failed"));
            }
        } catch (Exception e) {
            Timber.e(e);
            sendBroadcast();
        }
    }

    // TODO: 20/09/2017 Here we should probably parse this info into objects rather than doing it manually
    // TODO: 20/09/2017 Get a list of all possible payloads construct objects
    private void attemptParseMessage(String message, JSONObject jsonObject) {
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
                                if (payloadDataManager.getWallet().containsLegacyAddress((String) prevOutObj.get("addr"))) {
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
                        if (outObj.has("addr") && objX.has("hash")) {
                            rxBus.emitEvent(WebSocketReceiveEvent.class, new WebSocketReceiveEvent(
                                    (String) outObj.get("addr"),
                                    (String) objX.get("hash")
                            ));
                        }
                        if (outObj.has("xpub")) {
                            totalValue += value;
                        } else if (outObj.has("addr")) {
                            if (payloadDataManager.getWallet().containsLegacyAddress((String) outObj.get("addr"))) {
                                totalValue += value;
                            }
                        }
                    }
                }

                String title = context.getString(R.string.app_name);
                if (totalValue > 0L) {
                    String marquee = context.getString(R.string.received_bitcoin)
                            + " "
                            + monetaryUtil.getBtcFormat().format((double) totalValue / 1e8)
                            + " BTC";
                    String text = marquee;
                    if (totalValue > 0) {
                        text += " "
                                + context.getString(R.string.from).toLowerCase()
                                + " "
                                + inAddr;
                    }

                    triggerNotification(title, marquee, text);
                }

                updateBalancesAndTransactions();

            } else if (op.equals("on_change")) {
                final String localChecksum = payloadDataManager.getPayloadChecksum();
                boolean isSameChecksum = false;
                if (jsonObject.has("x")) {
                    JSONObject x = jsonObject.getJSONObject("x");
                    if (x.has("checksum")) {
                        final String remoteChecksum = x.getString("checksum");
                        isSameChecksum = remoteChecksum.equals(localChecksum);
                    }
                }

                if (!onChangeHashSet.contains(message) && !isSameChecksum) {
                    // Remote update to wallet data detected
                    if (payloadDataManager.getTempPassword() != null) {
                        // Download changed payload
                        //noinspection ThrowableResultOfMethodCallIgnored
                        downloadChangedPayload().subscribe(
                                () -> showToast().subscribe(new IgnorableDefaultObserver<>()),
                                throwable -> Timber.e(throwable, "downloadChangedPayload failed"));
                    }

                    onChangeHashSet.add(message);
                }
            } else if (message.contains("account_sub")) {
                attemptParseEthMessage(message);
            }
        } catch (Exception e) {
            Timber.e(e, "attemptParseMessage");
        }
    }

    private Completable showToast() {
        return Completable.fromRunnable(
                () -> ToastCustom.makeText(
                        context,
                        context.getString(R.string.wallet_updated),
                        ToastCustom.LENGTH_SHORT,
                        ToastCustom.TYPE_GENERAL))
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private Completable downloadChangedPayload() {
        return Completable.fromCallable(() -> {
            payloadDataManager.initializeAndDecrypt(
                    payloadDataManager.getWallet().getSharedKey(),
                    payloadDataManager.getWallet().getGuid(),
                    payloadDataManager.getTempPassword()).subscribe(this::updateBalancesAndTransactions);
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private Observable<CombinedEthModel> downloadEthTransactions() {
        return ethDataManager.fetchEthAddress()
                .compose(RxUtil.applySchedulersToObservable());
    }

    private void triggerNotification(String title, String marquee, String text) {
        Intent notifyIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        new NotificationsUtil(context, notificationManager).triggerNotification(
                title,
                marquee,
                text,
                R.drawable.ic_notification_white,
                pendingIntent,
                1000);
    }

}
