package piuk.blockchain.android.ui.chooser;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityAccountChooserBinding;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseAuthActivity;


public class AccountChooserActivity extends BaseAuthActivity implements AccountChooserViewModel.DataListener {

    public static final String EXTRA_REQUEST_TYPE = "request_type";
    public static final int REQUEST_CODE_CHOOSE_ACCOUNT = 2017;

    private ActivityAccountChooserBinding binding;
    private AccountChooserViewModel viewModel;
    private PaymentRequestType paymentRequestType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_chooser);

        viewModel = new AccountChooserViewModel(this, Locale.getDefault());

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_REQUEST_TYPE) || intent.getSerializableExtra(EXTRA_REQUEST_TYPE) == null) {
            throw new AssertionError("Request type must be passed to AccountChooserActivity");
        } else {
            paymentRequestType = (PaymentRequestType) intent.getSerializableExtra(EXTRA_REQUEST_TYPE);

            if (paymentRequestType.equals(PaymentRequestType.SEND)) {
                binding.toolbar.toolbarGeneral.setTitle(R.string.to);
            } else {
                binding.toolbar.toolbarGeneral.setTitle(R.string.from);
            }

            setSupportActionBar(binding.toolbar.toolbarGeneral);
            if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel.onViewReady();
    }

    @Override
    public PaymentRequestType getPaymentRequestType() {
        return paymentRequestType;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void updateUi(List<ItemAccount> items) {
        AccountChooserAdapter adapter = new AccountChooserAdapter(items, object -> {
            Log.d(getClass().getSimpleName(), "updateUi: " + object);
        });
        binding.recyclerview.setAdapter(adapter);
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
    }

    // TODO: 31/01/2017 Main Activity is going to have to pass this result to it's child fragments :(
    public static void startForResult(Activity context, PaymentRequestType paymentRequestType) {
        Intent starter = new Intent(context, AccountChooserActivity.class);
        starter.putExtra(EXTRA_REQUEST_TYPE, paymentRequestType);
        context.startActivityForResult(starter, REQUEST_CODE_CHOOSE_ACCOUNT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}
