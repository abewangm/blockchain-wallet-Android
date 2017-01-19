package piuk.blockchain.android.ui.contacts.detail;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityContactDetailBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.ViewUtils;

public class ContactDetailActivity extends BaseAuthActivity implements ContactDetailViewModel.DataListener {

    private ActivityContactDetailBinding binding;
    private ContactDetailViewModel viewModel;
    private MaterialProgressDialog progressDialog;
    private ContactTransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ContactDetailViewModel(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_detail);

        binding.toolbar.toolbarGeneral.setTitle("");
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonDelete.setOnClickListener(v -> viewModel.onDeleteContactClicked());
        binding.buttonRename.setOnClickListener(v -> viewModel.onRenameContactClicked());
        binding.buttonSend.setOnClickListener(v -> viewModel.onSendMoneyClicked());
        binding.buttonRequest.setOnClickListener(v -> viewModel.onRequestMoneyClicked());

        transactionAdapter = new ContactTransactionAdapter(new ArrayList<>(), new StringUtils(this));
        transactionAdapter.setClickListener(id -> viewModel.onTransactionClicked(id));

        binding.recyclerView.setAdapter(transactionAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel.onViewReady();
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public void finishPage() {
        finish();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showRenameDialog(String name) {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setHint(R.string.name);
        int maxLength = 128;
        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxLength);
        editText.setFilters(fArray);
        editText.setText(name);
        editText.setSelection(name.length());

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, editText))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.onContactRenamed(editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showDeleteUserDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_delete)
                .setMessage(R.string.contacts_delete_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.onDeleteContactConfirmed())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void disablePayments() {
        binding.buttonRequest.setEnabled(false);
        binding.buttonSend.setEnabled(false);
    }

    @Override
    public void onTransactionsUpdated(List<FacilitatedTransaction> transactions) {
        transactionAdapter.onTransactionsUpdated(transactions);
        if (!transactions.isEmpty()) {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.layoutNoTransactions.setVisibility(View.GONE);
        } else {
            binding.recyclerView.setVisibility(View.GONE);
            binding.layoutNoTransactions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void startPaymentRequestActivity(PaymentRequestType paymentRequestType, String contactId) {
        ContactPaymentRequestActivity.start(this, paymentRequestType, contactId);
    }

    @Override
    public void showProgressDialog() {
        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(R.string.please_wait);
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
    public void showAccountChoiceDialog(List<String> accounts, String fctxId) {
        AppCompatSpinner spinner = new AppCompatSpinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, accounts));
        final int[] selection = {0};
        //noinspection AnonymousInnerClassMayBeStatic
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selection[0] = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        FrameLayout frameLayout = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginInPixels = (int) ViewUtils.convertDpToPixel(20, this);
        params.setMargins(marginInPixels, 0, marginInPixels, 0);
        frameLayout.addView(spinner, params);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_choose_account_message)
                .setView(frameLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.onAccountChosen(selection[0], fctxId))
                .create()
                .show();
    }

    @Override
    public void initiatePayment(String uri, Contact contact) {
        Log.d("Lol", "initiatePayment: " + uri);
    }

    @Override
    public void updateContactName(String name) {
        binding.toolbar.toolbarGeneral.setTitle(name);
        setSupportActionBar(binding.toolbar.toolbarGeneral);

        binding.buttonRequest.setText(getString(R.string.contacts_request_bitcoin, name));
        binding.buttonSend.setText(getString(R.string.contacts_send_bitcoin, name));
        binding.textviewTransactionListHeader.setText(getString(R.string.contacts_detail_transactions, name));
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

}
