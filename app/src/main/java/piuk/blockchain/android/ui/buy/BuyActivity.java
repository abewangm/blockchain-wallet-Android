package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.annotations.Thunk;

public class BuyActivity extends BaseAuthActivity implements BuyViewModel.DataListener, FrontendJavascript<String> {

    public static final String TAG = BuyActivity.class.getSimpleName();
    private FrontendJavascriptManager frontendJavascriptManager;

    private Boolean frontendInitialized = false;
    private WebViewLoginDetails webViewLoginDetails;
    private Boolean didBuyBitcoin = false;

    private ActivityBuyBinding binding;
    private BuyViewModel viewModel;

    @Thunk MaterialProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);
        viewModel = new BuyViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.onboarding_buy_bitcoin);

        WebView webView = binding.webview;

        if (AndroidUtils.is21orHigher()) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        frontendJavascriptManager = new FrontendJavascriptManager(this, webView);

        webView.addJavascriptInterface(frontendJavascriptManager, FrontendJavascriptManager.JS_INTERFACE_NAME);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.restoreState(getIntent().getParcelableExtra(MainActivity.WEB_VIEW_STATE_KEY));

        showProgressDialog(R.string.please_wait);
        viewModel.onViewReady();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        frontendJavascriptManager.teardown();
        WebView webView = binding.webview;
        webView.removeJavascriptInterface(FrontendJavascriptManager.JS_INTERFACE_NAME);

        if (didBuyBitcoin) {
            viewModel.reloadExchangeDate();
        }
        dismissProgressDialog();
    }

    @Override
    public void onReceiveValue(String value) {
        Log.d(TAG, "Received JS value: " + value);
    }

    public void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails) {
        Log.d(TAG, "setWebViewLoginDetails: done");
        this.webViewLoginDetails = webViewLoginDetails;
        activateIfReady();
    }

    @Override
    public void onFrontendInitialized() {
        Log.d(TAG, "onFrontendInitialized: done");
        frontendInitialized = true;
        activateIfReady();
    }

    @Override
    public void onBuyCompleted() {
        Log.d(TAG, "onBuyCompleted: done");
        didBuyBitcoin = true;
    }

    @Override
    public void onShowTx(String txHash) {
        Bundle bundle = new Bundle();
        bundle.putString(BalanceFragment.KEY_TRANSACTION_HASH, txHash);
        TransactionDetailActivity.start(this, bundle);
    }

    private void activateIfReady() {
        if (isReady()) {
            frontendJavascriptManager.activateMobileBuyFromJson(
                    webViewLoginDetails,
                    viewModel.isNewlyCreated()
            );
            dismissProgressDialog();
        }
    }

    public boolean isReady() {
        return frontendInitialized && webViewLoginDetails != null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Handled in {@link MainActivity}
     */
    @Override
    public void onCompletedTrade(String txHash) {
        // no-op
    }

    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();
        if (!isFinishing()) {
            progress = new MaterialProgressDialog(this);
            progress.setMessage(message);
            progress.setCancelable(false);
            progress.show();
        }
    }

    public void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
            progress = null;
        }
    }
}
