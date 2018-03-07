package piuk.blockchain.android.ui.confirm;

import javax.inject.Inject;

import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.base.UiState;

public class ConfirmPaymentPresenter extends BasePresenter<ConfirmPaymentView> {

    private static final String AMOUNT_FORMAT = "%1$s %2$s (%3$s%4$s)";

    @Inject
    ConfirmPaymentPresenter() {
        // Empty Constructor
    }

    @Override
    public void onViewReady() {
        PaymentConfirmationDetails paymentDetails = getView().getPaymentDetails();

        if (paymentDetails == null) {
            getView().closeDialog();
            return;
        }

        String contactNote = getView().getContactNote();
        if (contactNote != null) {
            getView().setContactNote(contactNote);
        }

        getView().setFromLabel(paymentDetails.fromLabel);
        getView().setToLabel(paymentDetails.toLabel);
        getView().setAmount(String.format(AMOUNT_FORMAT,
                paymentDetails.cryptoAmount,
                paymentDetails.cryptoUnit,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatAmount));
        getView().setFee(String.format(AMOUNT_FORMAT,
                paymentDetails.cryptoFee,
                paymentDetails.cryptoUnit,
                paymentDetails.fiatSymbol,
                paymentDetails.fiatFee));
        getView().setTotalBtc(paymentDetails.cryptoTotal + " " + paymentDetails.cryptoUnit);
        getView().setTotalFiat(paymentDetails.fiatSymbol + paymentDetails.fiatTotal);

        if (paymentDetails.warningText != null) {
            getView().setWarning(paymentDetails.warningText);
            getView().setWarningSubText(paymentDetails.warningSubtext);
        }

        getView().setUiState(UiState.CONTENT);
    }

}
