package piuk.blockchain.android.ui.buy;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

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

    public void activateMobileBuy(String uid, String sharedKey, String password) {
        String script = FrontendJavascriptManager.createActivateScript(uid, sharedKey, password);
        executeScript(script);
    }

    private void executeScript(String script) {
        Log.d(TAG, "Executing: " + script);
        webView.post(() -> webView.evaluateJavascript(script, frontendJavascript));
    }

    public static String createActivateScript(String uid, String sharedKey, String password) {
        return String.format("activateMobileBuy('%s','%s','%s')", uid, sharedKey, password);
    }
}
