package piuk.blockchain.android.ui.contacts.payments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;

import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.DialogPayContactBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.SecondPasswordHandler;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.base.BaseDialogFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.AddressAdapter;
import piuk.blockchain.android.util.annotations.Thunk;


public class ContactPaymentDialog extends BaseDialogFragment<ContactPaymentDialogView, ContactPaymentDialogPresenter>
        implements ContactPaymentDialogView {

    public static final String ARGUMENT_URI = "uri";
    public static final String ARGUMENT_CONTACT_ID = "contact_id";
    public static final String ARGUMENT_CONTACT_MDID = "contact_mdid";
    public static final String ARGUMENT_FCTX_ID = "fctx_id";

    @Inject ContactPaymentDialogPresenter paymentDialogPresenter;
    private OnContactPaymentDialogInteractionListener listener;
    private MaterialProgressDialog progressDialog;
    @Thunk DialogPayContactBinding binding;
    @Thunk AlertDialog transactionSuccessDialog;
    private final Handler dialogHandler = new Handler();
    private final Runnable dialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (transactionSuccessDialog != null && transactionSuccessDialog.isShowing()) {
                transactionSuccessDialog.dismiss();
            }
        }
    };

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    public static ContactPaymentDialog newInstance(String uri,
                                                   String contactId,
                                                   String mdid,
                                                   String fctxId) {
        ContactPaymentDialog fragment = new ContactPaymentDialog();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_URI, uri);
        args.putString(ARGUMENT_CONTACT_ID, contactId);
        args.putString(ARGUMENT_CONTACT_MDID, mdid);
        args.putString(ARGUMENT_FCTX_ID, fctxId);
        fragment.setArguments(args);
        fragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_pay_contact, container, false);
        binding.getRoot().setFocusableInTouchMode(true);
        binding.getRoot().requestFocus();
        setCancelable(true);
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
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setNavigationOnClickListener(v -> dismiss());
        CharSequence charSequence = CalligraphyUtils.applyTypefaceSpan(
                getString(R.string.send_payment),
                TypefaceUtils.load(getContext().getAssets(), "fonts/Montserrat-Regular.ttf"));
        binding.toolbar.setTitle(charSequence);

        onViewReady();

        AddressAdapter receiveToAdapter = new AddressAdapter(
                getActivity(),
                R.layout.spinner_item,
                getPresenter().getSendFromList(),
                true);
        receiveToAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        binding.spinnerFrom.spinner.setAdapter(receiveToAdapter);
        binding.spinnerFrom.spinner.setSelection(getPresenter().getDefaultAccountPosition());
        binding.spinnerFrom.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                binding.spinnerFrom.spinner.setSelection(binding.spinnerFrom.spinner.getSelectedItemPosition());
                getPresenter().accountSelected(binding.spinnerFrom.spinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // No-op
            }
        });

        binding.spinnerFrom.spinner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onGlobalLayout() {
                binding.spinnerFrom.spinner.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                binding.spinnerFrom.spinner.setDropDownWidth(binding.spinnerFrom.spinner.getWidth());
            }
        });

        binding.buttonSend.setOnClickListener(v -> onSendClicked());
    }

    @Override
    public Bundle getFragmentBundle() {
        return getArguments();
    }

    @Override
    public void setContactName(String name) {
        binding.labelTo.setText(name);
    }

    @Override
    public void updatePaymentAmountBtc(String amount) {
        binding.labelPaymentAmountBtc.setText(amount);
    }

    @Override
    public void updateFeeAmountFiat(String amount) {
        binding.labelFeeAmountFiat.setText(amount);
    }

    @Override
    public void updatePaymentAmountFiat(String amount) {
        binding.labelPaymentAmountFiat.setText(amount);
    }

    @Override
    public void updateFeeAmountBtc(String amount) {
        binding.labelFeeAmountBtc.setText(amount);
    }

    @Override
    public void setPaymentButtonEnabled(boolean enabled) {
        binding.buttonSend.setEnabled(enabled);
    }

    @Override
    public void onUiUpdated() {
        binding.loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void finishPage(boolean paymentMade) {
        listener.onContactPaymentDialogClosed(paymentMade);
        dismiss();
    }

    @Override
    public void onShowTransactionSuccess(String contactMdid, String hash, String fctxId, long amount) {
        Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

        View dialogView = View.inflate(getActivity(), R.layout.modal_transaction_success, null);
        transactionSuccessDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setTitle(R.string.transaction_submitted)
                .setPositiveButton(getString(R.string.done), null)
                .setOnDismissListener(dialog -> finishAndNotifySuccess(contactMdid, hash, fctxId, amount))
                .create();

        transactionSuccessDialog.show();
        dialogHandler.postDelayed(dialogRunnable, 5 * 1000);
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
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getActivity(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    protected ContactPaymentDialogPresenter createPresenter() {
        return paymentDialogPresenter;
    }

    @Override
    protected ContactPaymentDialogView getMvpView() {
        return this;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnContactPaymentDialogInteractionListener) {
            listener = (OnContactPaymentDialogInteractionListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnContactPaymentDialogInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private void onSendClicked() {
        SecondPasswordHandler handler = new SecondPasswordHandler(getContext());
        handler.validate(new SecondPasswordHandler.ResultListener() {
            @Override
            public void onNoSecondPassword() {
                getPresenter().onSendClicked(null);
            }

            @Override
            public void onSecondPasswordValidated(String validatedSecondPassword) {
                getPresenter().onSendClicked(validatedSecondPassword);
            }
        });
    }

    private void finishAndNotifySuccess(@Nullable String mdid, String hash, @Nullable String fctxId, long transactionValue) {
        listener.onSendPaymentSuccessful(mdid, hash, fctxId, transactionValue);
        finishPage(true);
    }

    public interface OnContactPaymentDialogInteractionListener {

        void onContactPaymentDialogClosed(boolean paymentToContactMade);

        void onSendPaymentSuccessful(@Nullable String mdid, String transactionHash, @Nullable String fctxId, long transactionValue);

    }

}
