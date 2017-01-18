package piuk.blockchain.android.ui.contacts.pairing;


import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentContactsInvitationBuilderSenderBinding;
import piuk.blockchain.android.util.annotations.Thunk;

public class ContactsInvitationBuilderSenderFragment extends Fragment {

    private static final String KEY_BUNDLE_NAME = "bundle_name";
    @Thunk FragmentContactsInvitationBuilderSenderBinding binding;
    private FragmentInteractionListener listener;

    public ContactsInvitationBuilderSenderFragment() {
        // Required empty public constructor
    }

    public static ContactsInvitationBuilderSenderFragment newInstance(String name) {
        Bundle args = new Bundle();
        args.putString(KEY_BUNDLE_NAME, name);
        ContactsInvitationBuilderSenderFragment fragment = new ContactsInvitationBuilderSenderFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contacts_invitation_builder_sender, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String name = getArguments().getString(KEY_BUNDLE_NAME);
        binding.textviewName.setText(getString(R.string.contacts_how_are_you_known_header, name));


        binding.buttonNext.setOnClickListener(v -> {
            if (binding.editTextName.getText().toString().isEmpty()) {
                binding.inputLayoutName.setError(getString(R.string.contacts_field_error_empty));
            } else {
                listener.onSenderNameSubmitted(binding.editTextName.getText().toString());
            }
        });

        // Reset error on new input
        binding.editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.inputLayoutName.setError(null);
            }
        });
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

        void onSenderNameSubmitted(String name);

    }
}
