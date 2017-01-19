package piuk.blockchain.android.ui.contacts.payments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.RelativeLayout;

import info.blockchain.wallet.util.CharSequenceX;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.AddressAdapter;
import piuk.blockchain.android.util.annotations.Thunk;

public class SendDialogFragment extends AppCompatDialogFragment
        implements SendDialogViewModel.DataListener {

    public static final String TAG = SendDialogFragment.class.getSimpleName();
    public static final String KEY_BUNDLE_URI = "uri";
    public static final String KEY_BUNDLE_CONTACT_ID = "contact_id";

    @Thunk SendDialogViewModel viewModel;
    private MaterialProgressDialog progressDialog;

    // Views
    @Thunk AppCompatSpinner fromSpinner;
    private AppCompatTextView toLabel;
    private AppCompatTextView transferAmountBtc;
    private AppCompatTextView transferAmountFiat;
    private AppCompatTextView feeAmountBtc;
    private AppCompatTextView feeAmountFiat;
    private AppCompatButton sendButton;
    // Layouts
    private RelativeLayout loadingLayout;
    private FragmentInteractionListener listener;

    public SendDialogFragment() {
        // Required empty constructor
    }

    public static SendDialogFragment newInstance(String uri, String contactId) {
        Bundle args = new Bundle();
        args.putString(KEY_BUNDLE_URI, uri);
        args.putString(KEY_BUNDLE_CONTACT_ID, contactId);
        SendDialogFragment fragment = new SendDialogFragment();
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_send_funds, container, false);
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
    public Bundle getBundle() {
        return getArguments();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> dismiss());
        toolbar.setTitle(R.string.transfer_confirm);

        viewModel = new SendDialogViewModel(this);

        toLabel = (AppCompatTextView) view.findViewById(R.id.label_to);
        fromSpinner = (AppCompatSpinner) view.findViewById(R.id.spinner_from);
        transferAmountBtc = (AppCompatTextView) view.findViewById(R.id.label_transfer_amount_btc);
        transferAmountFiat = (AppCompatTextView) view.findViewById(R.id.label_transfer_amount_fiat);
        feeAmountBtc = (AppCompatTextView) view.findViewById(R.id.label_fee_amount_btc);
        feeAmountFiat = (AppCompatTextView) view.findViewById(R.id.label_fee_amount_fiat);
        sendButton = (AppCompatButton) view.findViewById(R.id.button_send);

        loadingLayout = (RelativeLayout) view.findViewById(R.id.loading_layout);

        AddressAdapter sendFromAdapter = new AddressAdapter(
                getActivity(),
                R.layout.spinner_item,
                viewModel.getSendFromList(),
                true);
        sendFromAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        fromSpinner.setAdapter(sendFromAdapter);
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                fromSpinner.setSelection(fromSpinner.getSelectedItemPosition());
                viewModel.accountSelected(fromSpinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        fromSpinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    fromSpinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //Deprecated, but necessary to prevent issues on < 16
                    //noinspection deprecation
                    fromSpinner.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    fromSpinner.setDropDownWidth(fromSpinner.getWidth());
                }
            }
        });

        sendButton.setOnClickListener(v ->
                new SecondPasswordHandler(getActivity()).validate(new SecondPasswordHandler.ResultListener() {
                    @Override
                    public void onNoSecondPassword() {
                        viewModel.sendPayment(null);
                    }

                    @Override
                    public void onSecondPasswordValidated(String validateSecondPassword) {
                        viewModel.sendPayment(new CharSequenceX(validateSecondPassword));
                    }
                }));

        fromSpinner.setSelection(viewModel.getDefaultAccount());

        viewModel.onViewReady();
    }

    @Override
    public void showProgressDialog() {
        hideProgressDialog();
        progressDialog = new MaterialProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);
        if (getActivity() != null && !getActivity().isFinishing()) {
            progressDialog.show();
        }
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onUiUpdated() {
        loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void updateToLabel(String label) {
        toLabel.setText(label);
    }

    @Override
    public void updateTransferAmountBtc(String amount) {
        transferAmountBtc.setText(amount);
    }

    @Override
    public void updateTransferAmountFiat(String amount) {
        transferAmountFiat.setText(amount);
    }

    @Override
    public void updateFeeAmountBtc(String amount) {
        feeAmountBtc.setText(amount);
    }

    @Override
    public void updateFeeAmountFiat(String amount) {
        feeAmountFiat.setText(amount);
    }

    @Override
    public void setPaymentButtonEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
    }

    @Override
    public void dismissDialog() {
        Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
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
        viewModel.destroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentInteractionListener) {
            listener = (FragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement FragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface FragmentInteractionListener {

        void onPaymentSuccessful();

        void onPaymentDialogDismissed();

    }

}
