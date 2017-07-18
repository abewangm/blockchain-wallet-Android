package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsInvitationBuilderBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ContactsInvitationBuilderActivity extends BaseMvpActivity<ContactsInvitationBuilderView, ContactsInvitationBuilderPresenter>
        implements ContactsInvitationBuilderRecipientFragment.FragmentInteractionListener,
        ContactsInvitationBuilderSenderFragment.FragmentInteractionListener,
        ContactsInvitationShareMethodFragment.FragmentInteractionListener,
        ContactsInvitationBuilderQrFragment.FragmentInteractionListener,
        ContactsInvitationBuilderView {

    @Inject ContactsInvitationBuilderPresenter invitationBuilderPresenter;
    private MaterialProgressDialog progressDialog;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityContactsInvitationBuilderBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_contacts_invitation_builder);

        setupToolbar(binding.toolbar.toolbarGeneral, R.string.contacts_add_contact_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        submitFragmentTransaction(new ContactsInvitationBuilderRecipientFragment());

        onViewReady();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, ContactsInvitationBuilderActivity.class);
        context.startActivity(starter);
    }

    @Override
    public void onRecipientNameSubmitted(String name) {
        getPresenter().setNameOfRecipient(name);

        submitFragmentTransaction(ContactsInvitationBuilderSenderFragment.newInstance(name));
    }

    @Override
    public void onSenderNameSubmitted(String name) {
        getPresenter().setNameOfSender(name);

        submitFragmentTransaction(new ContactsInvitationShareMethodFragment());
    }

    @Override
    public void onQrCodeSelected() {
        getPresenter().onQrCodeSelected();
    }

    @Override
    public void onUriGenerated(String uri, String recipientName) {
        ContactsInvitationBuilderQrFragment fragment = ContactsInvitationBuilderQrFragment.newInstance(uri, recipientName);
        submitFragmentTransaction(fragment);
    }

    @Override
    public void onLinkSelected() {
        getPresenter().onLinkClicked();
    }

    @Override
    public void showProgressDialog() {
        dismissProgressDialog();
        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.please_wait));

        if (!isFinishing()) progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onLinkGenerated(Intent intent) {
        startActivity(Intent.createChooser(intent, getString(R.string.contacts_share_invitation)));
    }

    @Override
    public void onDoneSelected() {
        getPresenter().onDoneSelected();
    }

    @Override
    public void finishPage() {
        Intent intent = new Intent(this, ContactsListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected ContactsInvitationBuilderPresenter createPresenter() {
        return invitationBuilderPresenter;
    }

    @Override
    protected ContactsInvitationBuilderView getView() {
        return this;
    }

    private void submitFragmentTransaction(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment)
                .commit();
    }

}
