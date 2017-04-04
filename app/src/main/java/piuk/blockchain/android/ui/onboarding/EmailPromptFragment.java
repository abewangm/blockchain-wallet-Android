package piuk.blockchain.android.ui.onboarding;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentEmailPromptBinding;


public class EmailPromptFragment extends Fragment {

    private static final String ARGUMENT_EMAIL = "email";

    private OnFragmentInteractionListener listener;
    private FragmentEmailPromptBinding binding;

    public EmailPromptFragment() {
        // Required empty public constructor
    }

    public static EmailPromptFragment newInstance(String email) {
        EmailPromptFragment fragment = new EmailPromptFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_email_prompt, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonEnable.setOnClickListener(v -> {
            if (listener != null) listener.onVerifyEmailClicked();
        });

        binding.buttonLater.setOnClickListener(v -> {
            if (listener != null) listener.onVerifyLaterClicked();
        });

        if (getArguments() != null) {
            final String email = getArguments().getString(ARGUMENT_EMAIL);
            binding.textviewEmail.setText(email);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    interface OnFragmentInteractionListener {

        void onVerifyEmailClicked();

        void onVerifyLaterClicked();

    }

}
