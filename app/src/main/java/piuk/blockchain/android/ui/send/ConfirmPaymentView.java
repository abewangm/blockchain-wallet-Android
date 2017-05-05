package piuk.blockchain.android.ui.send;

import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.base.View;

interface ConfirmPaymentView extends View {

    // No-op
    PaymentConfirmationDetails getPaymentDetails();

    void setUiState(@UiState.UiStateDef int uiState);

    void setFromLabel(String fromLabel);

    void setToLabel(String toLabel);

    void setAmount(String amount);

    void setFee(String fee);

    void setTotalBtc(String totalBtc);

    void setTotalFiat(String totalFiat);

    void closeDialog();

    void setSendButtonEnabled(boolean enabled);

}
