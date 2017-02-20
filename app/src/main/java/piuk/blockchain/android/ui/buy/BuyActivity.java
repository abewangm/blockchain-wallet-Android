package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import java.io.IOException;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.util.annotations.Thunk;

public class BuyActivity extends BaseAuthActivity implements ValueCallback<String> {

    public static final String TAG = BuyActivity.class.getSimpleName();
    private final String JS_INTERFACE_NAME = "android";
    private final int METADATA_TYPE_EXTERNAL = 3;

    @Thunk
    ActivityBuyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);

        WebView webview = binding.webview;
        webview.addJavascriptInterface(new JsInterface(), JS_INTERFACE_NAME);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl("http://localhost:8080/wallet/#/intermediate/");
        webview.evaluateJavascript("console.log('in js land');", this);

        /*
            Wallet Credentials
            PayloadManager.getInstance().getPayload().getGuid()
            PayloadManager.getInstance().getPayload().getSharedKey()
            PayloadManager.getInstance().getTempPassword().toString()
        */
    }

    // metadata string from getMetadata, and set with setMetadata
    private Metadata getBuyMetadata() throws IOException, MetadataException {
        return new Metadata.Builder(PayloadManager.getInstance().getMasterKey(),
            METADATA_TYPE_EXTERNAL).build();
    }

    public void onReceiveValue(String value) {
        Log.d(TAG, "Received JS value: " + value);
    }
}

class JsInterface {
    public static final String TAG = JsInterface.class.getSimpleName();

    @JavascriptInterface
    public void frontendInitialized() {
        Log.d(TAG, "frontendInitialized");
    }
}
