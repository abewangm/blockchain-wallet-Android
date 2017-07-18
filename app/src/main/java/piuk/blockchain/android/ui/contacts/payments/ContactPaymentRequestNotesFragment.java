package piuk.blockchain.android.ui.contacts.payments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.FragmentContactPaymentRequestNotesBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseFragment;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ViewUtils;

public class ContactPaymentRequestNotesFragment extends BaseFragment<ContactPaymentRequestView, ContactsPaymentRequestPresenter>
        implements ContactPaymentRequestView {

    public static final String ARGUMENT_REQUEST_TYPE = "request_type";
    public static final String ARGUMENT_ACCOUNT_POSITION = "account_position";
    public static final String ARGUMENT_CONTACT_ID = "contact_id";
    public static final String ARGUMENT_SATOSHIS = "satoshis";

    @Inject ContactsPaymentRequestPresenter paymentRequestPresenter;
    private FragmentContactPaymentRequestNotesBinding binding;
    private MaterialProgressDialog progressDialog;
    private FragmentInteractionListener listener;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    public static ContactPaymentRequestNotesFragment newInstance(PaymentRequestType requestType,
                                                                 @Nullable Integer accountPosition,
                                                                 String contactId,
                                                                 long satoshis) {
        Bundle args = new Bundle();
        args.putSerializable(ARGUMENT_REQUEST_TYPE, requestType);
        args.putString(ARGUMENT_CONTACT_ID, contactId);
        args.putLong(ARGUMENT_SATOSHIS, satoshis);
        if (accountPosition != null) {
            args.putInt(ARGUMENT_ACCOUNT_POSITION, accountPosition);
        }
        ContactPaymentRequestNotesFragment fragment = new ContactPaymentRequestNotesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_payment_request_notes, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonNext.setOnClickListener(v -> {
            getPresenter().sendRequest();
            ViewUtils.hideKeyboard(getActivity());
        });

        onViewReady();
    }

    @Override
    public void contactLoaded(String name, PaymentRequestType paymentRequestType) {
        setSummary(paymentRequestType, name);
        setHint(paymentRequestType);
        binding.editTextNote.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus || !binding.editTextNote.getText().toString().isEmpty()) {
                binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_note_hint));
            } else {
                setHint(paymentRequestType);
            }
        });
    }

    @Override
    public Bundle getFragmentBundle() {
        return getArguments();
    }

    @Override
    public String getNote() {
        return binding.editTextNote.getText().toString();
    }

    @Override
    public void finishPage() {
        if (listener != null) listener.onPageFinished();
    }

    @Override
    public void showProgressDialog() {
        progressDialog = new MaterialProgressDialog(getContext());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(R.string.please_wait);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getContext(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showSendSuccessfulDialog(String name) {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle(getString(R.string.contacts_payment_success_waiting_title, name))
                .setMessage(R.string.contacts_payment_success_waiting_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finishPage())
                .show();
    }

    @Override
    public void showRequestSuccessfulDialog() {
        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_payment_success_request_sent_title)
                .setMessage(R.string.contacts_payment_success_waiting_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finishPage())
                .show();
    }

    @Override
    protected ContactsPaymentRequestPresenter createPresenter() {
        return paymentRequestPresenter;
    }

    @Override
    protected ContactPaymentRequestView getMvpView() {
        return this;
    }

    private void setSummary(PaymentRequestType paymentRequestType, String contactName) {
        if (paymentRequestType.equals(PaymentRequestType.SEND)) {
            binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_send_note, contactName));
        } else {
            binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_receive_note, contactName));
        }
    }

    private void setHint(PaymentRequestType paymentRequestType) {
        if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {
            binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_receive_hint));
        } else {
            binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_send_hint));
        }
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

        void onPageFinished();

    }

}
