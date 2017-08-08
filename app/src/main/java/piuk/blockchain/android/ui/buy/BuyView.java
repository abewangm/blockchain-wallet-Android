package piuk.blockchain.android.ui.buy;

import android.support.annotation.StringRes;

import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface BuyView extends View {

    void setUiState(@UiState.UiStateDef int uiState);

    void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);

    void showSecondPasswordDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);
}
