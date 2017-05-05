package piuk.blockchain.android.ui.send;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import org.apache.commons.lang3.NotImplementedException;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.DialogConfirmTransactionBinding;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BaseDialogFragment;
import piuk.blockchain.android.ui.base.UiState;

public class ConfirmPaymentDialog extends BaseDialogFragment<ConfirmPaymentView, ConfirmPaymentPresenter>
        implements ConfirmPaymentView {

    private static final String ARGUMENT_PAYMENT_DETAILS = "argument_payment_details";

    private DialogConfirmTransactionBinding binding;

    public static ConfirmPaymentDialog newInstance(PaymentConfirmationDetails details) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PAYMENT_DETAILS, details);
        ConfirmPaymentDialog fragment = new ConfirmPaymentDialog();
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_confirm_transaction, container, false);
        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        getDialog().setCancelable(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> dismiss());

        binding.buttonChangeFee.setOnClickListener(v -> getPresenter().onChangeFeeClicked());
        binding.buttonSend.setOnClickListener(v -> getPresenter().onSendClicked());

        onViewReady();
    }

    @Override
    public void setFromLabel(String fromLabel) {
        binding.textviewFromAddress.setText(fromLabel);
    }

    @Override
    public void setToLabel(String toLabel) {
        binding.textviewToAddress.setText(toLabel);
    }

    @Override
    public void setAmount(String amount) {
        binding.textviewAmount.setText(amount);
    }

    @Override
    public void setFee(String fee) {
        binding.textviewFees.setText(fee);
    }

    @Override
    public void setTotalBtc(String totalBtc) {
        binding.textviewTotalBtc.setText(totalBtc);
    }

    @Override
    public void setTotalFiat(String totalFiat) {
        binding.textviewTotalFiat.setText(totalFiat);
    }

    @Override
    public void setSendButtonEnabled(boolean enabled) {
        binding.buttonSend.setEnabled(enabled);
    }

    @Override
    public void closeDialog() {
        dismiss();
    }

    @Override
    public PaymentConfirmationDetails getPaymentDetails() {
        return getArguments().getParcelable(ARGUMENT_PAYMENT_DETAILS);
    }

    @Override
    public void setUiState(int uiState) {
        switch (uiState) {
            case UiState.LOADING:
                binding.loadingLayout.setVisibility(View.VISIBLE);
                binding.mainLayout.setVisibility(View.GONE);
                break;
            case UiState.CONTENT:
                binding.loadingLayout.setVisibility(View.GONE);
                binding.mainLayout.setVisibility(View.VISIBLE);
                break;
            case UiState.EMPTY:
            case UiState.FAILURE:
                throw new NotImplementedException("State " + uiState + " hasn't been implemented yet");
        }
    }

    @Override
    protected ConfirmPaymentPresenter createPresenter() {
        return new ConfirmPaymentPresenter();
    }

    @Override
    protected ConfirmPaymentView getMvpView() {
        return this;
    }

}
