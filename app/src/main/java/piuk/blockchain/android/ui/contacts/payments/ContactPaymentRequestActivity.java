package piuk.blockchain.android.ui.contacts.payments;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityContactPaymentRequestBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ContactPaymentRequestActivity extends BaseAuthActivity implements
        ContactsPaymentRequestViewModel.DataListener,
        ContactPaymentRequestNotesFragment.FragmentInteractionListener,
        ContactPaymentRequestAmountFragment.FragmentInteractionListener {

    private static final String KEY_EXTRA_REQUEST_TYPE = "extra_request_type";
    private static final String KEY_EXTRA_CONTACT_ID = "extra_contact_id";

    private ActivityContactPaymentRequestBinding binding;
    private ContactsPaymentRequestViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_payment_request);
        viewModel = new ContactsPaymentRequestViewModel(this);

        binding.toolbar.toolbarGeneral.setTitle(R.string.contacts_payment_request_title);
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().hasExtra(KEY_EXTRA_REQUEST_TYPE)) {
            throw new AssertionError("Payment request type must be defined");
        } else if (!getIntent().hasExtra(KEY_EXTRA_CONTACT_ID)) {
            throw new AssertionError("Contact ID must be defined");
        }

        viewModel.onViewReady();

        viewModel.loadContact(getIntent().getStringExtra(KEY_EXTRA_CONTACT_ID));
    }

    @Override
    public void contactLoaded() {
        submitFragmentTransaction(
                ContactPaymentRequestNotesFragment.newInstance(
                        (PaymentRequestType) getIntent().getSerializableExtra(KEY_EXTRA_REQUEST_TYPE),
                        viewModel.getContactName()));
    }

    @Override
    public void finishPage() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    private void submitFragmentTransaction(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment)
                .commit();
    }

    public static void start(Context context, PaymentRequestType requestType, String contactId) {
        Intent starter = new Intent(context, ContactPaymentRequestActivity.class);
        starter.putExtra(KEY_EXTRA_REQUEST_TYPE, requestType);
        starter.putExtra(KEY_EXTRA_CONTACT_ID, contactId);
        context.startActivity(starter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onNextSelected(String note) {
        viewModel.onNoteSet(note);
        submitFragmentTransaction(
                ContactPaymentRequestAmountFragment.newInstance(
                        (PaymentRequestType) getIntent().getSerializableExtra(KEY_EXTRA_REQUEST_TYPE),
                        viewModel.getContactName()));
    }

    @Override
    public void onFinishSelected(long satoshis) {
        viewModel.onAmountSet(satoshis);
    }

}
