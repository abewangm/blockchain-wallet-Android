package piuk.blockchain.android.util;

import android.util.Log;

import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;


public class EventLogHandler {

    private static final String TAG = EventLogHandler.class.getSimpleName();

    public static final String URL_EVENT_BASE = "https://blockchain.info/event?name=";
    public static final String URL_EVENT_2ND_PW = URL_EVENT_BASE + "wallet_login_second_password_";
    public static final String URL_EVENT_LEGACY = URL_EVENT_BASE + "wallet_login_legacy_use_";
    public static final String URL_EVENT_BACKUP = URL_EVENT_BASE + "wallet_login_android_recovery_phrase_";

    public static final String URL_EVENT_TX_INPUT_FROM_QR = URL_EVENT_BASE + "wallet_android_tx_from_qr";
    public static final String URL_EVENT_TX_INPUT_FROM_PASTE = URL_EVENT_BASE + "wallet_android_tx_from_paste";
    public static final String URL_EVENT_TX_INPUT_FROM_URI = URL_EVENT_BASE + "wallet_android_tx_from_uri";
    public static final String URL_EVENT_TX_INPUT_FROM_DROPDOWN = URL_EVENT_BASE + "wallet_android_tx_from_dropdown";

    PrefsUtil prefsUtil;
    WebUtil webUtil;

    public EventLogHandler(PrefsUtil prefsUtil, WebUtil webUtil) {
        this.webUtil = webUtil;
        this.prefsUtil = prefsUtil;
    }

    public void log2ndPwEvent(boolean active) {
        logEventOnce(PrefsUtil.KEY_EVENT_2ND_PW, URL_EVENT_2ND_PW + getBoolean(active));
    }

    public void logLegacyEvent(boolean active) {
        // TODO: 20/12/2016 Disabled
//        logEventOnce(PrefsUtil.KEY_EVENT_LEGACY, URL_EVENT_LEGACY + getBoolean(active));
    }

    public void logBackupEvent(boolean active) {
        // TODO: 20/12/2016 Disabled
//        logEventOnce(PrefsUtil.KEY_EVENT_BACKUP, URL_EVENT_BACKUP + getBoolean(active));
    }

    public void logAddressInputEvent(String flag) {
        logEvent(flag);
    }

    private String getBoolean(boolean active) {
        if (active)
            return  "1";
        else
            return "0";
    }

    private void logEvent(String url) {

        sendEvent(url)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    if (response != null && FormatsUtil.getInstance().isValidJson(response)) {

                        try {
                            JSONObject responseJson = new JSONObject(response);
                            if(responseJson.has("success") && responseJson.getBoolean("success")) {
                                //success
                            } else {
                                Log.e(TAG, "Log Event Failed: "+url);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, Throwable::printStackTrace);
    }

    private void logEventOnce(String key, String url) {

        if (!prefsUtil.getValue(key, false)) {
            sendEvent(url)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    if (response != null && FormatsUtil.getInstance().isValidJson(response)) {

                        try {
                            JSONObject responseJson = new JSONObject(response);
                            if(responseJson.has("success") && responseJson.getBoolean("success")) {
                                prefsUtil.setValue(key, true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, Throwable::printStackTrace);
        }
    }

    private Observable<String> sendEvent(String url) {
        return Observable.fromCallable(() -> webUtil.getURL(url));
    }
}
