package piuk.blockchain.android.ui.backup;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.balance.LegacyBalanceFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.AddressAdapter;
import piuk.blockchain.android.util.annotations.Thunk;

public class ConfirmFundsTransferDialogFragment extends AppCompatDialogFragment
        implements ConfirmFundsTransferViewModel.DataListener {

    public static final String TAG = ConfirmFundsTransferDialogFragment.class.getSimpleName();

    @Thunk ConfirmFundsTransferViewModel mViewModel;
    private MaterialProgressDialog mProgressDialog;

    // Views
    @Thunk AppCompatSpinner mToSpinner;
    private TextView mFromLabel;
    private TextView mTransferAmountBtc;
    private TextView mTransferAmountFiat;
    private TextView mFeeAmountBtc;
    private TextView mFeeAmountFiat;
    private CheckBox mArchiveCheckbox;
    private Button mTransferButton;
    // Layouts
    private RelativeLayout mLoadingLayout;

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

        CharSequence charSequence = CalligraphyUtils.applyTypefaceSpan(
                getString(R.string.transfer_confirm),
                TypefaceUtils.load(getContext().getAssets(), "fonts/Montserrat-Regular.ttf"));
        toolbar.setTitle(charSequence);

        mViewModel = new ConfirmFundsTransferViewModel(this);

        mFromLabel = (TextView) view.findViewById(R.id.label_from);
        mToSpinner = (AppCompatSpinner) view.findViewById(R.id.spinner_destination);
        mTransferAmountBtc = (TextView) view.findViewById(R.id.label_transfer_amount_btc);
        mTransferAmountFiat = (TextView) view.findViewById(R.id.label_transfer_amount_fiat);
        mFeeAmountBtc = (TextView) view.findViewById(R.id.label_fee_amount_btc);
        mFeeAmountFiat = (TextView) view.findViewById(R.id.label_fee_amount_fiat);
        mArchiveCheckbox = (CheckBox) view.findViewById(R.id.checkbox_archive);
        mTransferButton = (Button) view.findViewById(R.id.button_transfer_all);

        mLoadingLayout = (RelativeLayout) view.findViewById(R.id.loading_layout);

        AddressAdapter receiveToAdapter = new AddressAdapter(
                getActivity(),
                R.layout.spinner_item,
                mViewModel.getReceiveToList(),
                true);
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mToSpinner.setAdapter(receiveToAdapter);
        mToSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mToSpinner.setSelection(mToSpinner.getSelectedItemPosition());
                mViewModel.accountSelected(mToSpinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        mToSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mToSpinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mToSpinner.setDropDownWidth(mToSpinner.getWidth());
            }
        });

        mTransferButton.setOnClickListener(v ->
                new SecondPasswordHandler(getActivity()).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        mViewModel.sendPayment(null);
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        mViewModel.sendPayment(validateSecondPassword);
                    }
                }));

        mToSpinner.setSelection(mViewModel.getDefaultAccount());

        mViewModel.onViewReady();
    }

    @Override
    public void showProgressDialog() {
        hideProgressDialog();
        mProgressDialog = new MaterialProgressDialog(getContext());
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setCancelable(false);
        if (getActivity() != null && !getActivity().isFinishing()) {
            mProgressDialog.show();
        }
    }

    @Override
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onUiUpdated() {
        mLoadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void updateFromLabel(String label) {
        mFromLabel.setText(label);
    }

    @Override
    public void updateTransferAmountBtc(String amount) {
        mTransferAmountBtc.setText(amount);
    }

    @Override
    public void updateTransferAmountFiat(String amount) {
        mTransferAmountFiat.setText(amount);
    }

    @Override
    public void updateFeeAmountBtc(String amount) {
        mFeeAmountBtc.setText(amount);
    }

    @Override
    public void updateFeeAmountFiat(String amount) {
        mFeeAmountFiat.setText(amount);
    }

    @Override
    public void setPaymentButtonEnabled(boolean enabled) {
        mTransferButton.setEnabled(enabled);
    }

    @Override
    public boolean getIfArchiveChecked() {
        return mArchiveCheckbox.isChecked();
    }

    @Override
    public void dismissDialog() {
        Intent intent = new Intent(LegacyBalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        dismiss();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
    }
}
