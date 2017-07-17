package piuk.blockchain.android.ui.chooser;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityAccountChooserBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseAuthActivity;


public class AccountChooserActivity extends BaseAuthActivity implements AccountChooserViewModel.DataListener {

    public static final String EXTRA_REQUEST_TYPE = "request_type";
    public static final String EXTRA_REQUEST_CODE = "request_code";
    public static final String EXTRA_SELECTED_ITEM = "selected_object";
    public static final String EXTRA_SELECTED_OBJECT_TYPE = "selected_object_type";
    public static final int REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE = 216;
    public static final int REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND = 217;
    public static final int REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND = 218;
    public static final int REQUEST_CODE_CHOOSE_CONTACT = 219;

    private ActivityAccountChooserBinding binding;
    private AccountChooserViewModel viewModel;
    private PaymentRequestType paymentRequestType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_chooser);

        viewModel = new AccountChooserViewModel(this);

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_REQUEST_TYPE) || intent.getSerializableExtra(EXTRA_REQUEST_TYPE) == null) {
            throw new AssertionError("Request type must be passed to AccountChooserActivity");
        } else {
            paymentRequestType = (PaymentRequestType) intent.getSerializableExtra(EXTRA_REQUEST_TYPE);
            int requestCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, -1);
            binding.toolbar.toolbarGeneral.setTitle(
                    requestCode == REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE
                            || requestCode == REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND
                            || requestCode == REQUEST_CODE_CHOOSE_CONTACT ? R.string.to : R.string.from);

            setSupportActionBar(binding.toolbar.toolbarGeneral);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel.onViewReady();
    }

    @Override
    public void showNoContacts() {
        binding.recyclerview.setVisibility(View.GONE);
        binding.layoutNoContacts.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public PaymentRequestType getPaymentRequestType() {
        return paymentRequestType;
    }

    // TODO: 17/07/2017 Remove me
    @Override
    public boolean getIfContactsEnabled() {
        return true;
    }

    @Override
    public void updateUi(List<ItemAccount> items) {
        AccountChooserAdapter adapter = new AccountChooserAdapter(items, object -> {
            if (object != null) {
                try {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_SELECTED_ITEM, new ObjectMapper().writeValueAsString(object));
                    intent.putExtra(EXTRA_SELECTED_OBJECT_TYPE, object.getClass().getName());
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
    }

    public static void startForResult(Fragment fragment, int requestCode, PaymentRequestType paymentRequestType) {
        Intent starter = new Intent(fragment.getContext(), AccountChooserActivity.class);
        starter.putExtra(EXTRA_REQUEST_TYPE, paymentRequestType);
        starter.putExtra(EXTRA_REQUEST_CODE, requestCode);
        fragment.startActivityForResult(starter, requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }

}
