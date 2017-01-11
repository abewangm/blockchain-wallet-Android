package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentContactsInvitationBuilderQrBinding;

public class ContactsInvitationBuilderQrFragment extends Fragment {

    public static final String KEY_BUNDLE_NAME = "bundle_name";
    private FragmentContactsInvitationBuilderQrBinding binding;
    private FragmentInteractionListener listener;

    public ContactsInvitationBuilderQrFragment() {
        // Required empty constructor
    }

    public static ContactsInvitationBuilderQrFragment newInstance(String name) {

        Bundle args = new Bundle();
        args.putString(KEY_BUNDLE_NAME, name);
        ContactsInvitationBuilderQrFragment fragment = new ContactsInvitationBuilderQrFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_contacts_invitation_builder_qr, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonDone.setOnClickListener(v -> listener.onDoneSelected());

        String name = getArguments().getString(KEY_BUNDLE_NAME);

        binding.textviewName.setText(String.format(getString(R.string.contacts_scan_instructions), name));
    }

    public void setQrCode(@NonNull Bitmap bitmap) {
        binding.qrCode.setImageBitmap(bitmap);
        binding.qrCode.setVisibility(View.VISIBLE);
        binding.progressbar.setVisibility(View.GONE);
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

        void onDoneSelected();

    }


}
