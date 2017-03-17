package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebView;
import info.blockchain.wallet.api.trade.SFOXApi;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.MetadataUtil;
import io.reactivex.disposables.CompositeDisposable;
import org.bitcoinj.crypto.DeterministicKey;
import org.spongycastle.util.encoders.Hex;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.annotations.Thunk;

public class BuyActivity extends BaseAuthActivity implements FrontendJavascript<String> {
    public static final String TAG = BuyActivity.class.getSimpleName();
    private final String JS_INTERFACE_NAME = "android";
    private final int METADATA_TYPE_EXTERNAL = 3;
    private FrontendJavascriptManager frontendJavascriptManager;
    private PayloadManager payloadManager;

    private Metadata buyMetadata = null;
    private Boolean frontendInitialized = false;
    private Boolean didBuyBitcoin = false;

    private SFOXApi sfoxApi;
    private CompositeDisposable compositeDisposable;

    @Thunk
    ActivityBuyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.buy);

        compositeDisposable = new CompositeDisposable();
        sfoxApi = new SFOXApi();

        WebView webView = binding.webview;
        frontendJavascriptManager = new FrontendJavascriptManager(this, webView);
        payloadManager = PayloadManager.getInstance();

        webView.addJavascriptInterface(frontendJavascriptManager, JS_INTERFACE_NAME);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.restoreState(getIntent().getParcelableExtra(MainActivity.WEB_VIEW_STATE_KEY));

        loadBuyMetadata();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        frontendJavascriptManager.teardown();
        WebView webView = binding.webview;
        webView.removeJavascriptInterface(JS_INTERFACE_NAME);

        if (didBuyBitcoin) {
            // Should reload buy metadata, watch for new trade
        }
    }

    private Metadata getBuyMetadata() throws Exception {
        DeterministicKey masterKey = payloadManager.getPayload().getHdWallets().get(0)
            .getMasterKey();
        DeterministicKey metadataHDNode = MetadataUtil.deriveMetadataNode(masterKey);
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

    public void onBuyCompleted() {
        Log.d(TAG, "onBuyCompleted: done");
        this.didBuyBitcoin = true;
    }

    private void activateIfReady() {
        if (this.isReady()) {
            try {
                String metadata = buyMetadata.getMetadata();
                byte[] magicHash = buyMetadata.getMagicHash();

                frontendJavascriptManager.activateMobileBuyFromJson(
                        payloadManager.getPayload().toJson(),
                        metadata == null ? "" : metadata,
                        magicHash == null ? "" : Hex.toHexString(magicHash),
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void fetchTransaction(String accountToken) {

        compositeDisposable.add(
            sfoxApi.getTransactions(accountToken)
                .compose(RxUtil.applySchedulersToObservable())
                .subscribe(
                    response -> {
                        //do stuff
//                        response.getAction();
//                        response.getAmount();
//                        response.getTransactionId();
                    },
                    Throwable::printStackTrace));
    }
}
