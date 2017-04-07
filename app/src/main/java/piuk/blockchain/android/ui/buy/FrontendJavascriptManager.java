package piuk.blockchain.android.ui.buy;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Created by justin on 2/22/17.
 */

public class FrontendJavascriptManager {
    public static final String TAG = FrontendJavascriptManager.class.getSimpleName();

    private FrontendJavascript frontendJavascript;
    private WebView webView;

    FrontendJavascriptManager(FrontendJavascript frontendJavascript, WebView webView) {
        this.frontendJavascript = frontendJavascript;
        this.webView = webView;
    }

    @JavascriptInterface
    public void frontendInitialized() {
        frontendJavascript.onFrontendInitialized();
    }

    @JavascriptInterface
    public void buyCompleted() {
        frontendJavascript.onBuyCompleted();
    }

    public void activateMobileBuy(String uid, String sharedKey, String password) {
        String script = FrontendJavascriptManager.createActivateScript(uid, sharedKey, password);
        executeScript(script);
    }

    public void activateMobileBuyFromJson(String walletJson, String externalJson, String magicHash, String password) {
        String script = FrontendJavascriptManager.createActivateFromJsonScript(walletJson, externalJson, magicHash, password);
        executeScript(script);
    }

    public void teardown() {
        executeScript("teardown()");
    }

    private void executeScript(String script) {
        Log.d(TAG, "Executing: " + script);
        // TODO: 06/04/2017 evaluateJavascript isn't available on pre-19 devices
        webView.post(() -> webView.evaluateJavascript(script, frontendJavascript));
    }

    public static String createActivateScript(String uid, String sharedKey, String password) {
        return String.format(
                "activateMobileBuy('%s','%s','%s')",
                StringEscapeUtils.escapeEcmaScript(uid),
                StringEscapeUtils.escapeEcmaScript(sharedKey),
                StringEscapeUtils.escapeEcmaScript(password)
        );
    }

    public static String createActivateFromJsonScript(String walletJson, String externalJson, String magicHash, String password) {
        return String.format(
                "activateMobileBuyFromJson('%s','%s','%s','%s')",
                StringEscapeUtils.escapeEcmaScript(walletJson),
                StringEscapeUtils.escapeEcmaScript(externalJson),
                StringEscapeUtils.escapeEcmaScript(magicHash),
                StringEscapeUtils.escapeEcmaScript(password)
        );
    }
}
