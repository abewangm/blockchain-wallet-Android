package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputFilter;
import android.text.InputType;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactDetailBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ViewUtils;

public class ContactDetailActivity extends BaseAuthActivity implements ContactDetailViewModel.DataListener {

    private ActivityContactDetailBinding binding;
    private ContactDetailViewModel viewModel;
    private MaterialProgressDialog progressDialog;

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
