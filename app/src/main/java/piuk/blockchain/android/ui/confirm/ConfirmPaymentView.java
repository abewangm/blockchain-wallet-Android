package piuk.blockchain.android.ui.confirm;

import android.support.annotation.Nullable;

import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.base.View;

interface ConfirmPaymentView extends View {

    PaymentConfirmationDetails getPaymentDetails();

    @Nullable String getContactNote();

    void setUiState(@UiState.UiStateDef int uiState);

    void setFromLabel(String fromLabel);

    void setToLabel(String toLabel);

    void setAmount(String amount);

    void setFee(String fee);

    void setTotalBtc(String totalBtc);

    void setTotalFiat(String totalFiat);

    void closeDialog();

    void setContactNote(String contactNote);

    void setWarning(String warning);

    void setWarningSubText(String warningSubText);
}
