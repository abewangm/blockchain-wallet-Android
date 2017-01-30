package piuk.blockchain.android.ui.contacts.payments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.FragmentContactPaymentRequestNotesBinding;

public class ContactPaymentRequestNotesFragment extends Fragment {

    public static final String ARGUMENT_REQUEST_TYPE = "request_type";
    public static final String ARGUMENT_CONTACT_NAME = "contact_name";

    private FragmentContactPaymentRequestNotesBinding binding;
    private FragmentInteractionListener listener;

    public ContactPaymentRequestNotesFragment() {
        // Required empty constructor
    }

    public static ContactPaymentRequestNotesFragment newInstance(PaymentRequestType requestType, String contactName) {
        Bundle args = new Bundle();
        args.putSerializable(ARGUMENT_REQUEST_TYPE, requestType);
        args.putString(ARGUMENT_CONTACT_NAME, contactName);
        ContactPaymentRequestNotesFragment fragment = new ContactPaymentRequestNotesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_payment_request_notes, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String contactName = getArguments().getString(ARGUMENT_CONTACT_NAME);
        PaymentRequestType paymentRequestType =
                (PaymentRequestType) getArguments().getSerializable(ARGUMENT_REQUEST_TYPE);

        if (paymentRequestType != null) {
            if (paymentRequestType.equals(PaymentRequestType.SEND)) {
                binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_send_note, contactName));
            } else {
                binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_receive_note, contactName));
            }

            setHint(paymentRequestType);

            binding.editTextNote.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_note_hint));
                } else {
                    setHint(paymentRequestType);
                }
            });

        } else {
            throw new AssertionError("Contact name and PaymentRequestType must be passed to fragment");
        }

        binding.buttonNext.setOnClickListener(v -> listener.onNextSelected(binding.editTextNote.getText().toString()));
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

    interface FragmentInteractionListener {

        void onNextSelected(String note);

    }

}
