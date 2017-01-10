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
import piuk.blockchain.android.databinding.FragmentContactsInvitationBuilderShareMethodBinding;

public class ContactsInvitationBuilderShareMethod extends Fragment {

    private FragmentContactsInvitationBuilderShareMethodBinding binding;
    private FragmentInteractionListener listener;

    public ContactsInvitationBuilderShareMethod() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contacts_invitation_builder_share_method, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Quit process
        binding.buttonDone.setOnClickListener(v -> listener.onDoneSelected());

        binding.buttonQr.setOnClickListener(v -> listener.onQrCodeSelected());

        binding.buttonLink.setOnClickListener(v -> listener.onLinkSelected());
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

        void onQrCodeSelected();

        void onLinkSelected();

        void onDoneSelected();

    }
}
