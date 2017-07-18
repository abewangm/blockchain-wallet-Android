package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.FragmentContactsInvitationBuilderQrBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseFragment;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ContactsInvitationBuilderQrFragment extends BaseFragment<ContactsQrView, ContactsQrPresenter>
        implements ContactsQrView {

    public static final String ARGUMENT_URI = "uri";
    public static final String ARGUMENT_NAME = "name";

    @Inject ContactsQrPresenter contactsQrPresenter;
    private FragmentContactsInvitationBuilderQrBinding binding;
    private FragmentInteractionListener listener;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    public static ContactsInvitationBuilderQrFragment newInstance(String uri, String name) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_URI, uri);
        args.putString(ARGUMENT_NAME, name);
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

        binding.buttonDone.setOnClickListener(v -> finishPage());

        getPresenter().onViewReady();
    }

    @Override
    public Bundle getFragmentBundle() {
        return getArguments();
    }

    @Override
    public void updateDisplayMessage(String name) {
        binding.textviewName.setText(getString(R.string.contacts_scan_instructions, name));
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(getContext(), getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onQrLoaded(Bitmap bitmap) {
        binding.qrCode.setImageBitmap(bitmap);
        binding.qrCode.setVisibility(View.VISIBLE);
        binding.progressbar.setVisibility(View.GONE);
    }

    @Override
    public void finishPage() {
        listener.onDoneSelected();
    }

    @Override
    protected ContactsQrPresenter createPresenter() {
        return contactsQrPresenter;
    }

    @Override
    protected ContactsQrView getMvpView() {
        return this;
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
