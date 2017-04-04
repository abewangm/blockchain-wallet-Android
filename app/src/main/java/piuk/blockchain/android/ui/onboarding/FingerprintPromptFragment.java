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
import piuk.blockchain.android.databinding.FragmentFingerprintPromptBinding;

public class FingerprintPromptFragment extends Fragment {

    private OnFragmentInteractionListener listener;
    private FragmentFingerprintPromptBinding binding;

    public FingerprintPromptFragment() {
        // Required empty public constructor
    }

    public static FingerprintPromptFragment newInstance() {
        return new FingerprintPromptFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_fingerprint_prompt,
                container,
                false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonEnable.setOnClickListener(v -> {
            if (listener != null) listener.onEnableFingerprintClicked();
        });
        binding.buttonLater.setOnClickListener(v -> {
            if (listener != null) listener.onCompleteLaterClicked();
        });
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

        void onEnableFingerprintClicked();

        void onCompleteLaterClicked();

    }

}
