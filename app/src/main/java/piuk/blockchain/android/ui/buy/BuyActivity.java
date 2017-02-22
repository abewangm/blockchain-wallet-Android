package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import java.io.IOException;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.util.annotations.Thunk;

public class BuyActivity extends BaseAuthActivity implements FrontendJavascript<String> {
    public static final String TAG = BuyActivity.class.getSimpleName();
    private final String JS_INTERFACE_NAME = "android";
    private final int METADATA_TYPE_EXTERNAL = 3;
    private FrontendJavascriptManager frontendJavascriptManager;
    private PayloadManager payloadManager;

    @Thunk
    ActivityBuyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);

        WebView webView = binding.webview;
        frontendJavascriptManager = new FrontendJavascriptManager(this, webView);
        payloadManager = PayloadManager.getInstance();

        webView.addJavascriptInterface(frontendJavascriptManager, JS_INTERFACE_NAME);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://localhost:8080/wallet/#/intermediate");
    }

    private Metadata getBuyMetadata() throws IOException, MetadataException {
        return new Metadata.Builder(PayloadManager.getInstance().getMasterKey(),
            METADATA_TYPE_EXTERNAL).build();
    }

    public void onReceiveValue(String value) {
        Log.d(TAG, "Received JS value: " + value);
    }

    public void onFrontendInitialized() {
        Log.d(TAG, "Frontend initialized");
        frontendJavascriptManager.activateMobileBuy(
                payloadManager.getPayload().getGuid(),
                payloadManager.getPayload().getSharedKey(),
                payloadManager.getTempPassword().toString()
        );
    }
}
