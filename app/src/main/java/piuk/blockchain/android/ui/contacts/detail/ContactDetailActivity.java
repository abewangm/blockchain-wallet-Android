package piuk.blockchain.android.ui.contacts.detail;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactDetailBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.SendFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailActivity;

import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID;

public class ContactDetailActivity extends BaseAuthActivity implements
        ContactDetailActivityViewModel.DataListener,
        ContactDetailFragment.OnFragmentInteractionListener,
        SendFragment.OnSendFragmentInteractionListener {

    private ActivityContactDetailBinding binding;
    private MaterialProgressDialog progressDialog;
    private ContactDetailActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_detail);
        viewModel = new ContactDetailActivityViewModel(this);

        binding.toolbar.toolbarGeneral.setTitle("");
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null && getIntent().hasExtra(KEY_BUNDLE_CONTACT_ID)) {
            submitFragmentTransaction(
                    ContactDetailFragment.newInstance(
                            getIntent().getStringExtra(KEY_BUNDLE_CONTACT_ID)));
        } else {
            finish();
            return;
        }

        viewModel.onViewReady();
    }

    private void submitFragmentTransaction(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.content_frame, fragment)
                .commit();
    }

    public Toolbar getToolbar() {
        return binding.toolbar.toolbarGeneral;
    }

    public static void start(Context context, @NonNull Bundle extras) {
        Intent starter = new Intent(context, ContactDetailActivity.class);
        starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onFinishPageCalled() {
        finish();
    }

    @Override
    public void onPaymentInitiated(String uri, String recipientId, String mdid, String fctxId, boolean isBtc, int defaultIndex) {
        SendFragment sendFragment = SendFragment.newInstance(uri, recipientId, mdid, fctxId, isBtc, defaultIndex);
        submitFragmentTransaction(sendFragment);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragmentById = fragmentManager.findFragmentById(R.id.content_frame);
        if (fragmentById != null && fragmentById instanceof SendFragment) {
            submitFragmentTransaction(
                    ContactDetailFragment.newInstance(
                            getIntent().getStringExtra(KEY_BUNDLE_CONTACT_ID)));
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showProgressDialog(@StringRes int string) {
        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(string);
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_payment_sent_failed_message)
                .setPositiveButton(R.string.retry, (dialog, which) ->
                        viewModel.broadcastPaymentSuccess(mdid, txHash, facilitatedTxId))
                .setCancelable(false)
                .create()
                .show();
    }

    @Override
    public void showPaymentMismatchDialog(@StringRes int message) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void onSendPaymentSuccessful(@Nullable String mdid, String transactionHash, @Nullable String fctxId) {
        viewModel.broadcastPaymentSuccess(mdid, transactionHash, fctxId);
    }

    @Override
    public void dismissPaymentPage() {
        onBackPressed();
    }

    @Override
    public void onShowTransactionDetailCalled(String hash) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_TRANSACTION_HASH, hash);
        TransactionDetailActivity.start(this, bundle);
    }

    @Override
    public void onSendFragmentClose() {
        // No-op
    }

    @Override
    public void onSendFragmentStart() {
        // No-op
    }

}
