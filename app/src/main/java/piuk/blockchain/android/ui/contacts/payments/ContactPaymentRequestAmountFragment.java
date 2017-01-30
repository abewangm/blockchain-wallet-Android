package piuk.blockchain.android.ui.contacts.payments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.FragmentContactPaymentRequestAmountBinding;
import piuk.blockchain.android.util.annotations.Thunk;


public class ContactPaymentRequestAmountFragment extends Fragment implements ContactRequestAmountViewModel.DataListener {

    private static final String TAG = ContactPaymentRequestAmountFragment.class.getSimpleName();
    private static final String ARGUMENT_REQUEST_TYPE = "request_type";
    private static final String ARGUMENT_CONTACT_NAME = "contact_name";

    @Thunk FragmentContactPaymentRequestAmountBinding binding;
    @Thunk ContactRequestAmountViewModel viewModel;
    @Thunk boolean textChangeAllowed = true;
    private FragmentInteractionListener listener;

    public static ContactPaymentRequestAmountFragment newInstance(PaymentRequestType requestType, String contactName) {
        Bundle args = new Bundle();
        args.putSerializable(ARGUMENT_REQUEST_TYPE, requestType);
        args.putString(ARGUMENT_CONTACT_NAME, contactName);
        ContactPaymentRequestAmountFragment fragment = new ContactPaymentRequestAmountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contact_payment_request_amount, container, false);

        return binding.getRoot();
    }

    private TextWatcher btcTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // No-op
        }

        @Override
        public void afterTextChanged(Editable s) {
            binding.inputLayoutAmountBtc.setError(null);

            String input = s.toString();

            binding.editTextAmountBtc.removeTextChangedListener(this);
            NumberFormat btcFormat = NumberFormat.getInstance(Locale.getDefault());
            btcFormat.setMaximumFractionDigits(viewModel.getCurrencyHelper().getMaxBtcDecimalLength() + 1);
            btcFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, viewModel.getCurrencyHelper().getMaxBtcDecimalLength(), binding.editTextAmountBtc);

            binding.editTextAmountBtc.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateFiatTextField(s.toString());

                textChangeAllowed = true;
            }
            setKeyListener(s, binding.editTextAmountBtc);
        }
    };

    private TextWatcher fiatTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            binding.inputLayoutAmountFiat.setError(null);

            String input = s.toString();

            binding.editTextAmountFiat.removeTextChangedListener(this);
            int maxLength = 2;
            NumberFormat fiatFormat = NumberFormat.getInstance(Locale.getDefault());
            fiatFormat.setMaximumFractionDigits(maxLength + 1);
            fiatFormat.setMinimumFractionDigits(0);

            s = formatEditable(s, input, maxLength, binding.editTextAmountFiat);

            binding.editTextAmountFiat.addTextChangedListener(this);

            if (textChangeAllowed) {
                textChangeAllowed = false;
                viewModel.updateBtcTextField(s.toString());
                textChangeAllowed = true;
            }
            setKeyListener(s, binding.editTextAmountFiat);
        }
    };

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ContactRequestAmountViewModel(this, Locale.getDefault());

        String contactName = getArguments().getString(ARGUMENT_CONTACT_NAME);
        PaymentRequestType paymentRequestType =
                (PaymentRequestType) getArguments().getSerializable(ARGUMENT_REQUEST_TYPE);

        if (paymentRequestType == null || contactName == null) {
            throw new AssertionError("Contact name and PaymentRequestType must be passed to fragment");
        }

        if (paymentRequestType.equals(PaymentRequestType.SEND)) {
            binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_send_amount, contactName));
            binding.layoutReceiveAccount.setVisibility(View.GONE);
        } else {
            binding.textviewExplanation.setText(getString(R.string.contacts_payment_request_receive_amount, contactName));

            List<String> accounts = viewModel.getAccountsList();
            if (accounts.size() == 1) {
                // Hide account selection if only one account exists
                binding.layoutReceiveAccount.setVisibility(View.GONE);
                viewModel.setAccountPosition(0);
            } else {
                // Show dropdown for account selection
                binding.spinnerAccount.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, accounts));
                //noinspection AnonymousInnerClassMayBeStatic
                binding.spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        viewModel.setAccountPosition(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // No-op
                    }
                });
            }
        }

        binding.buttonNext.setOnClickListener(v -> {
            String btc = binding.editTextAmountBtc.getText().toString();
            if (btc.isEmpty()) {
                binding.inputLayoutAmountBtc.setError(getString(R.string.contacts_field_error_empty));
            } else if (btc.equals("0") || btc.equals("0.00")) {
                binding.inputLayoutAmountBtc.setError(getString(R.string.invalid_amount));
            } else {
                listener.onFinishSelected(viewModel.getAmountInSatoshis(), viewModel.getAccountPosition());
            }

            String fiat = binding.editTextAmountFiat.getText().toString();
            if (fiat.isEmpty()) {
                binding.inputLayoutAmountFiat.setError(getString(R.string.contacts_field_error_empty));
            } else if (fiat.equals("0") || fiat.equals("0.00")) {
                binding.inputLayoutAmountFiat.setError(getString(R.string.invalid_amount));
            }
        });

        // BTC Field
        binding.editTextAmountBtc.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.inputLayoutAmountBtc.setHint(
                getString(R.string.contacts_payment_request_amount_hint)
                        + " ("
                        + viewModel.getCurrencyHelper().getBtcUnit()
                        + ")");
        binding.editTextAmountBtc.addTextChangedListener(btcTextWatcher);

        // Fiat Field
        binding.editTextAmountFiat.setKeyListener(
                DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        binding.inputLayoutAmountFiat.setHint(
                getString(R.string.contacts_payment_request_amount_hint)
                        + " ("
                        + viewModel.getCurrencyHelper().getFiatUnit()
                        + ")");
        binding.editTextAmountFiat.addTextChangedListener(fiatTextWatcher);

        viewModel.onViewReady();
    }

    @Thunk
    void setKeyListener(Editable s, EditText editText) {
        if (s.toString().contains(getDefaultDecimalSeparator())) {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        } else {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789" + getDefaultDecimalSeparator()));
        }
    }

    @Thunk
    Editable formatEditable(Editable s, String input, int maxLength, EditText editText) {
        try {
            if (input.contains(getDefaultDecimalSeparator())) {
                String dec = input.substring(input.indexOf(getDefaultDecimalSeparator()));
                if (dec.length() > 0) {
                    dec = dec.substring(1);
                    if (dec.length() > maxLength) {
                        editText.setText(input.substring(0, input.length() - 1));
                        editText.setSelection(editText.getText().length());
                        s = editText.getEditableText();
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "afterTextChanged: ", e);
        }
        return s;
    }

    private String getDefaultDecimalSeparator() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        return Character.toString(symbols.getDecimalSeparator());
    }

    @Override
    public void updateBtcTextField(String text) {
        binding.editTextAmountBtc.setText(text);
    }

    @Override
    public void updateFiatTextField(String text) {
        binding.editTextAmountFiat.setText(text);
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
    public void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    interface FragmentInteractionListener {

        void onFinishSelected(long satoshis, int accountPosition);

    }
}
