package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ViewUtils;


public class ContactsActivity extends BaseAuthActivity implements ContactsViewModel.DataListener {

    private static final int REQUEST_PAIRING = 98;
    private ActivityContactsBinding binding;
    private ContactsViewModel viewModel;
    private ContactsListAdapter contactsListAdapter;
    private MaterialProgressDialog materialProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        viewModel = new ContactsViewModel(this);

        binding.toolbar.setTitle(R.string.contacts_title);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.fab.setOnClickListener(view -> startPairingActivity());

        binding.buttonDisplayQr.setOnClickListener(view -> viewModel.onViewQrClicked());

        binding.buttonRetry.setOnClickListener(view -> viewModel.onViewReady());

        contactsListAdapter = new ContactsListAdapter(new ArrayList<>());
        contactsListAdapter.setContactsClickListener(this::showDialogForContact);
        binding.layoutContent.setAdapter(contactsListAdapter);
        binding.layoutContent.setLayoutManager(new LinearLayoutManager(this));

        viewModel.onViewReady();
    }

    private void showDialogForContact(String mdid) {
        ArrayList<String> options = new ArrayList<>();
        options.add("Send money");
        options.add("Request money");
        options.add("Rename Contact");
        options.add("Delete Contact");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setAdapter(arrayAdapter, (dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            viewModel.onSendMoneyClicked(mdid);
                            break;
                        case 1:
                            viewModel.onRequestMoneyClicked(mdid);
                            break;
                        case 2:
                            viewModel.onRenameContactClicked(mdid);
                            break;
                        case 3:
                            viewModel.onDeleteContactClicked(mdid);
                            break;
                    }
                })
                .setPositiveButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onContactsLoaded(@NonNull List<ContactsListItem> contacts) {
        contactsListAdapter.onContactsUpdated(contacts);
    }

    @Override
    public void showQrCode(@NonNull Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    public void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Static method to assist with launching this activity
     */
    public static void start(Context context) {
        Intent starter = new Intent(context, ContactsActivity.class);
        context.startActivity(starter);
    }

    private void startPairingActivity() {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        editText.setHint(R.string.contacts_name_field_hint);

        final String[] name = new String[1];

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.contacts_add_contact_title)
                .setView(ViewUtils.getAlertDialogEditTextLayout(this, editText))
                .setPositiveButton(R.string.contacts_add_button, (dialogInterface, i) -> {
                    name[0] = editText.getText().toString().trim();
                    if (name[0].isEmpty()) {
                        ToastCustom.makeText(this, getString(R.string.contacts_name_cannot_be_empty), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                    } else {
                        Intent intent = new Intent(this, ContactPairingMethodActivity.class);
                        intent.putExtra(ContactPairingMethodActivity.INTENT_KEY_CONTACT_NAME, name[0]);
                        startActivityForResult(intent, REQUEST_PAIRING);
                    }
                })
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PAIRING && resultCode == RESULT_OK) {
            viewModel.onViewReady();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

    @Override
    public void showProgressDialog() {
        materialProgressDialog = new MaterialProgressDialog(this);
        materialProgressDialog.setCancelable(false);
        materialProgressDialog.setMessage(R.string.please_wait);
        materialProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (materialProgressDialog != null) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOADING, CONTENT, FAILURE, EMPTY})
    @interface UiState {
    }

    public static final int LOADING = 0;
    public static final int CONTENT = 1;
    public static final int FAILURE = 2;
    public static final int EMPTY = 3;

    @Override
    public void setUiState(@UiState int uiState) {
        switch (uiState) {
            case LOADING:
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case CONTENT:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.VISIBLE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case FAILURE:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case EMPTY:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                break;

        }
    }
}
