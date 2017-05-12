package piuk.blockchain.android.ui.buy;

import android.webkit.ValueCallback;

/**
 * Created by justin on 2/22/17.
 */

public interface FrontendJavascript<T> extends ValueCallback<T> {
    void onFrontendInitialized();
    void onBuyCompleted();
    void onCompletedTrade(String txHash);
    void onShowTx(String txHash);
}
