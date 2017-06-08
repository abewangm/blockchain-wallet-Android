package piuk.blockchain.android.ui.buy;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.apache.commons.lang3.StringEscapeUtils;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;

/**
 * Created by justin on 2/22/17.
 */

public class FrontendJavascriptManager {

    public static final String TAG = FrontendJavascriptManager.class.getSimpleName();
    public static final String JS_INTERFACE_NAME = "android";

    private FrontendJavascript frontendJavascript;
    private WebView webView;

    public FrontendJavascriptManager(FrontendJavascript frontendJavascript, WebView webView) {
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

    @JavascriptInterface
    public void completedTrade(String txHash) {
        frontendJavascript.onCompletedTrade(txHash);
    }

    @JavascriptInterface
    public void showTx(String txHash) {
        frontendJavascript.onShowTx(txHash);
    }

    void activateMobileBuyFromJson(WebViewLoginDetails webViewLoginDetails, boolean firstLogin) {
        String script = createActivateFromJsonScript(webViewLoginDetails, firstLogin);
        executeScript(script);
    }

    public void checkForCompletedTrades(WebViewLoginDetails webViewLoginDetails) {
        String script = createCheckForCompletedTradesScript(webViewLoginDetails);
        executeScript(script);
    }

    void teardown() {
        executeScript("teardown()");
    }

    private void executeScript(String script) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Executing: " + script);
        }
        new Handler(Looper.getMainLooper()).post(() -> webView.evaluateJavascript(script, frontendJavascript));
    }

    private String createActivateFromJsonScript(WebViewLoginDetails webViewLoginDetails, boolean firstLogin) {
        return String.format(
                "activateMobileBuyFromJson('%s','%s','%s','%s', %b)",
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getWalletJson()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getExternalJson()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getMagicHash()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getPassword()),
                firstLogin);
    }

    private String createCheckForCompletedTradesScript(WebViewLoginDetails webViewLoginDetails) {
        return String.format(
                "checkForCompletedTrades('%s','%s','%s','%s')",
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getWalletJson()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getExternalJson()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getMagicHash()),
                StringEscapeUtils.escapeEcmaScript(webViewLoginDetails.getPassword()));
    }
}
