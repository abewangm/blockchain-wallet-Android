package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentContactPaymentRequestBinding;
import piuk.blockchain.android.ui.contacts.ContactPaymentRequestActivity.PaymentRequestType;

public class ContactPaymentRequestNotesFragment extends Fragment {

    public static final String KEY_BUNDLE_REQUEST_TYPE = "request_type";
    public static final String KEY_BUNDLE_CONTACT_NAME = "contact_name";

    private FragmentContactPaymentRequestBinding binding;
    private FragmentInteractionListener listener;

    public ContactPaymentRequestNotesFragment() {
        // Required empty constructor
    }

    public static ContactPaymentRequestNotesFragment newInstance(PaymentRequestType requestType, String contactName) {

        Bundle args = new Bundle();
        args.putSerializable(KEY_BUNDLE_REQUEST_TYPE, requestType);
        args.putString(KEY_BUNDLE_CONTACT_NAME, contactName);
        ContactPaymentRequestNotesFragment fragment = new ContactPaymentRequestNotesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_payment_request, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String contactName = getArguments().getString(KEY_BUNDLE_CONTACT_NAME);
        PaymentRequestType paymentRequestType =
                (PaymentRequestType) getArguments().getSerializable(KEY_BUNDLE_REQUEST_TYPE);

        if (paymentRequestType != null) {
            if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {
                binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_send, contactName));
                binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_receive_hint));
            } else {
                binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_receive, contactName));
                binding.inputLayoutNote.setHint(getString(R.string.contacts_payment_request_send_hint));
            }
        } else {
            throw new AssertionError("Contact name and PaymentRequestType must be passed to fragment");
        }

        binding.buttonNext.setOnClickListener(v -> listener.onNextSelected(binding.editTextNote.getText().toString()));
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
