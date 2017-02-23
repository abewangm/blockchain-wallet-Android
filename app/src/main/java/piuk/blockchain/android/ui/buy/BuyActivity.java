package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import info.blockchain.wallet.util.MetadataUtil;
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

    private Metadata buyMetadata = null;
    private Boolean frontendInitialized = false;

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

        loadBuyMetadata();
    }
    
    private Metadata getBuyMetadata() throws IOException, MetadataException, NoSuchAlgorithmException {
        DeterministicKey metadataHDNode = MetadataUtil.deriveMetadataNode(payloadManager.getMasterKey());
        return new Metadata.Builder(metadataHDNode, METADATA_TYPE_EXTERNAL).build();
    }

    private void loadBuyMetadata() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Metadata buyMetadata = getBuyMetadata();
                    onMetadataLoaded(buyMetadata);
                } catch (Exception e) {
                    Log.d(TAG, "loadBuyMetadata error: " + e.getMessage());
                }
            }
        }.start();
    }

    public void onReceiveValue(String value) {
        Log.d(TAG, "Received JS value: " + value);
    }

    private void onMetadataLoaded(Metadata buyMetadata) {
        Log.d(TAG, "onMetadataLoaded: done");
        this.buyMetadata = buyMetadata;
        activateIfReady();
    }

    public void onFrontendInitialized() {
        Log.d(TAG, "onFrontendInitialized: done");
        this.frontendInitialized = true;
        activateIfReady();
    }

    private void activateIfReady() {
        if (this.isReady()) {
            try {
                frontendJavascriptManager.activateMobileBuyFromJson(
                        payloadManager.getPayload().getDecryptedPayload(),
                        buyMetadata.getMetadata(),
                        Hex.toHexString(buyMetadata.getMagicHash()),
                        payloadManager.getTempPassword().toString()
                );
            } catch (Exception e) {
                Log.d(TAG, "activateIfReady error: " + e.getMessage());
            }
        }
    }

    public boolean isReady() {
        return this.frontendInitialized && this.buyMetadata != null;
    }
}
