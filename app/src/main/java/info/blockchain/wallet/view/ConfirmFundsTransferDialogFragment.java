package info.blockchain.wallet.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.MonetaryUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.view.helpers.ReselectSpinner;
import info.blockchain.wallet.view.helpers.TransferFundsHelper;

import piuk.blockchain.android.R;

public class ConfirmFundsTransferDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = ConfirmFundsTransferDialogFragment.class.getSimpleName();

    private TransferFundsHelper mFundsHelper;
    private AppCompatTextView mFromLabel;
    private AppCompatTextView mToLabel;
    private ReselectSpinner mToSpinner;
    private AppCompatTextView mTransferAmountBtc;
    private AppCompatTextView mTransferAmountFiat;
    private AppCompatTextView mFeeAmountBtc;
    private AppCompatTextView mFeeAmountFiat;
    private AppCompatCheckBox mArchiveCheckbox;
    private AppCompatButton mTransferButton;

    public static ConfirmFundsTransferDialogFragment newInstance() {
        ConfirmFundsTransferDialogFragment fragment = new ConfirmFundsTransferDialogFragment();
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_transfer_funds, container, false);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
        getDialog().setCancelable(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> dismiss());
        toolbar.setTitle(R.string.transfer_confirm);

        mFromLabel = (AppCompatTextView) view.findViewById(R.id.label_from);
        mToLabel = (AppCompatTextView) view.findViewById(R.id.label_destination);
        mToSpinner = (ReselectSpinner) view.findViewById(R.id.spinner_destination);
        mTransferAmountBtc = (AppCompatTextView) view.findViewById(R.id.label_transfer_amount_btc);
        mTransferAmountFiat = (AppCompatTextView) view.findViewById(R.id.label_transfer_amount_fiat);
        mFeeAmountBtc = (AppCompatTextView) view.findViewById(R.id.label_fee_amount_btc);
        mFeeAmountFiat = (AppCompatTextView) view.findViewById(R.id.label_fee_amount_fiat);
        mArchiveCheckbox = (AppCompatCheckBox) view.findViewById(R.id.checkbox_archive);
        mTransferButton = (AppCompatButton) view.findViewById(R.id.button_transfer_all);

        mFundsHelper = new TransferFundsHelper(PayloadManager.getInstance());
        mFundsHelper.getTransferableFundTransactionList()
                .subscribe(pendingTransactions -> {
                    mFromLabel.setText(getResources().getQuantityString(R.plurals.transfer_label_plural, pendingTransactions.size()));
                    // TODO: 20/09/2016 Dropdown for receive to
                }, throwable -> {
                    throwable.printStackTrace();
                    dismiss();
                });

        PrefsUtil prefsUtil = new PrefsUtil(getActivity());
        MonetaryUtil monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        String fiatUnit = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double exchangeRate = ExchangeRateFactory.getInstance().getLastPrice(fiatUnit);

        long totalToSend = mFundsHelper.getTotalToSend();
        long totalFee = mFundsHelper.getTotalFee();
        String fiatAmount = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalToSend / 1e8));
        String fiatFee = monetaryUtil.getFiatFormat(fiatUnit).format(exchangeRate * ((double) totalFee / 1e8));

        int defaultIndex = PayloadManager.getInstance().getPayload().getHdWallet().getDefaultIndex();
        Account defaultAccount = PayloadManager.getInstance().getPayload().getHdWallet().getAccounts().get(defaultIndex);
        mFromLabel.setText(defaultAccount.getLabel() + " (" + getResources().getString(R.string.default_label) + ")");
        mTransferAmountBtc.setText(monetaryUtil.getDisplayAmountWithFormatting(totalToSend));
        mTransferAmountFiat.setText(fiatAmount);

        mFeeAmountBtc.setText(monetaryUtil.getDisplayAmountWithFormatting(totalFee));
        mFeeAmountFiat.setText(fiatFee);
    }
}
