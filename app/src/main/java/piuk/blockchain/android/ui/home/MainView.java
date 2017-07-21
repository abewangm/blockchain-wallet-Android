package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;

import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.ui.base.View;

interface MainView extends View {

    boolean isBuySellPermitted();

    void onScanInput(String strUri);

    void onStartContactsActivity(@Nullable String data);

    void onStartBalanceFragment(boolean paymentToContactMade);

    void kickToLauncherPage();

    void showProgressDialog(@StringRes int message);

    void hideProgressDialog();

    void clearAllDynamicShortcuts();

    void showMetadataNodeRegistrationFailure();

    void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId, long transactionValue);

    void showBroadcastSuccessDialog();

    void updateCurrentPrice(String price);

    void setBuySellEnabled(boolean enabled);

    void onTradeCompleted(String txHash);

    void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);

    void showCustomPrompt(AppCompatDialogFragment alertFragment);

    Context getActivityContext();

}
