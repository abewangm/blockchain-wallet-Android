package piuk.blockchain.android.ui.send;

import info.blockchain.wallet.api.data.FeeOptions;

import java.math.BigInteger;
import java.util.Arrays;

import javax.inject.Inject;

import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.util.SSLVerifyUtil;

public class ConfirmPaymentPresenter extends BasePresenter<ConfirmPaymentView> {

    private static final String AMOUNT_FORMAT = "%1$s %2$s (%3$s%4$s)";

    private PaymentConfirmationDetails details;
    private SendModel sendModel;

    @Inject SendDataManager sendDataManager;
    @Inject SSLVerifyUtil sslVerifyUtil;
    @Inject DynamicFeeCache dynamicFeeCache;

    ConfirmPaymentPresenter() {
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    @Override
    public void onViewReady() {
        sslVerifyUtil.validateSSL();

        details = getView().getPaymentDetails();
        sendModel = getView().getSendModel();

        if (details == null || sendModel == null) {
            getView().closeDialog();
            return;
        }

        getView().setFromLabel(details.fromLabel);
        getView().setToLabel(details.toLabel);
        getView().setAmount(String.format(AMOUNT_FORMAT,
                details.btcAmount,
                details.btcUnit,
                details.fiatSymbol,
                details.fiatAmount));
        getView().setFee(String.format(AMOUNT_FORMAT,
                details.btcFee,
                details.btcUnit,
                details.fiatSymbol,
                details.fiatFee));
        getView().setTotalBtc(details.btcTotal + " " + details.btcUnit);
        getView().setTotalFiat(details.fiatSymbol + details.fiatTotal);
        getView().setUiState(UiState.CONTENT);
    }

    void onChangeFeeClicked() {
        FeeOptions feeOptions = dynamicFeeCache.getFeeOptions();
        if (feeOptions == null) throw new RuntimeException("Fees cannot be null at this point");

        BigInteger regularFeeSize = sendDataManager.estimatedFee(
                sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(),
                1,
                BigInteger.valueOf(feeOptions.getRegularFee() * 1000));

        BigInteger priorityFeeSize = sendDataManager.estimatedFee(
                sendModel.pendingTransaction.unspentOutputBundle.getSpendableOutputs().size(),
                1,
                BigInteger.valueOf(feeOptions.getPriorityFee() * 1000));

        getView().showFeeChangeDialog(Arrays.asList(
                regularFeeSize.toString(),
                priorityFeeSize.toString()));

    }

    void onSendClicked() {
        getView().setSendButtonEnabled(false);
        // TODO: 05/05/2017
    }

}
