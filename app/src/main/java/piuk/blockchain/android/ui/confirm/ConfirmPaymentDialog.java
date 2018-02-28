package piuk.blockchain.android.ui.confirm;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import org.apache.commons.lang3.NotImplementedException;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.DialogConfirmTransactionBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails;
import piuk.blockchain.android.ui.base.BaseDialogFragment;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.util.AndroidUtils;

public class ConfirmPaymentDialog extends BaseDialogFragment<ConfirmPaymentView, ConfirmPaymentPresenter>
        implements ConfirmPaymentView {

    @Inject ConfirmPaymentPresenter confirmPaymentPresenter;

    private static final String ARGUMENT_PAYMENT_DETAILS = "ARGUMENT_PAYMENT_DETAILS";
    private static final String ARGUMENT_CONTACT_NOTE = "ARGUMENT_CONTACT_NOTE";
    private static final String ARGUMENT_SHOW_FEE_CHOICE = "ARGUMENT_SHOW_FEE_CHOICE";

    private DialogConfirmTransactionBinding binding;
    private OnConfirmDialogInteractionListener listener;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    public static ConfirmPaymentDialog newInstance(PaymentConfirmationDetails details,
                                                   @Nullable String note,
                                                   boolean showFeeChoice) {
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_PAYMENT_DETAILS, details);
        if (note != null) args.putString(ARGUMENT_CONTACT_NOTE, note);
        args.putBoolean(ARGUMENT_SHOW_FEE_CHOICE, showFeeChoice);
        ConfirmPaymentDialog fragment = new ConfirmPaymentDialog();
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        getDialog().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (AndroidUtils.is21orHigher()) {
            getDialog().getWindow().setStatusBarColor(
                    ContextCompat.getColor(getActivity(), R.color.primary_navy_dark));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v -> dismiss());
        binding.buttonChangeFee.setOnClickListener(v -> listener.onChangeFeeClicked());
        binding.buttonSend.setOnClickListener(v -> listener.onSendClicked());

        if (!getArguments().getBoolean(ARGUMENT_SHOW_FEE_CHOICE, true)) {
            binding.buttonChangeFee.setVisibility(View.GONE);
        }

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
    public void setContactNote(String contactNote) {
        binding.textviewContactNote.setText(contactNote);
        binding.textviewContactNote.setVisibility(View.VISIBLE);
        binding.textviewDescriptionHeader.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeDialog() {
        dismiss();
    }

    @Override
    public PaymentConfirmationDetails getPaymentDetails() {
        return getArguments().getParcelable(ARGUMENT_PAYMENT_DETAILS);
    }

    @Nullable
    @Override
    public String getContactNote() {
        return getArguments().getString(ARGUMENT_CONTACT_NOTE);
    }

    @Override
    public void setWarning(String warning) {
        binding.layoutWarning.setVisibility(View.VISIBLE);
        binding.textviewWarning.setText(warning);
    }

    @Override
    public void setWarningSubText(String warningSubText) {
        binding.textviewWarningSub.setText(warningSubText);
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
        return confirmPaymentPresenter;
    }

    @Override
    protected ConfirmPaymentView getMvpView() {
        return this;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnConfirmDialogInteractionListener) {
            listener = (OnConfirmDialogInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnConfirmDialogInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnConfirmDialogInteractionListener {

        void onChangeFeeClicked();

        void onSendClicked();

    }

}
