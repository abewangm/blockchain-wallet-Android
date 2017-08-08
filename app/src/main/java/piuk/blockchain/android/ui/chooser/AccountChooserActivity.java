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

import javax.inject.Inject;

import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.models.PaymentRequestType;
import piuk.blockchain.android.databinding.ActivityAccountChooserBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseMvpActivity;


public class AccountChooserActivity extends BaseMvpActivity<AccountChooserView, AccountChooserPresenter>
        implements AccountChooserView {

    public static final String EXTRA_REQUEST_TYPE = "request_type";
    public static final String EXTRA_REQUEST_CODE = "request_code";
    public static final String EXTRA_SELECTED_ITEM = "selected_object";
    public static final String EXTRA_SELECTED_OBJECT_TYPE = "selected_object_type";
    public static final String EXTRA_ACTIVITY_TITLE = "activity_title";
    public static final int REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE = 216;
    public static final int REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND = 217;
    public static final int REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND = 218;
    public static final int REQUEST_CODE_CHOOSE_CONTACT = 219;

    @Inject AccountChooserPresenter accountChooserPresenter;
    private ActivityAccountChooserBinding binding;
    private PaymentRequestType paymentRequestType;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_chooser);

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_REQUEST_TYPE) || intent.getSerializableExtra(EXTRA_REQUEST_TYPE) == null) {
            throw new AssertionError("Request type must be passed to AccountChooserActivity");
        } else {
            paymentRequestType = (PaymentRequestType) intent.getSerializableExtra(EXTRA_REQUEST_TYPE);
            int requestCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, -1);

            String title = getIntent().getStringExtra(EXTRA_ACTIVITY_TITLE);
            if (title == null) {
                throw new AssertionError("Title string must be passed to AccountChooserActivity");
            } else {
                binding.toolbar.toolbarGeneral.setTitle(title);
            }

            setSupportActionBar(binding.toolbar.toolbarGeneral);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        onViewReady();
    }

    @Override
    public void showNoContacts() {
        binding.recyclerview.setVisibility(View.GONE);
        binding.layoutNoContacts.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isContactsEnabled() {
        return BuildConfig.CONTACTS_ENABLED;
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

    public static void startForResult(Fragment fragment,
                                      int requestCode,
                                      PaymentRequestType paymentRequestType,
                                      String title) {

        Intent starter = new Intent(fragment.getContext(), AccountChooserActivity.class);
        starter.putExtra(EXTRA_REQUEST_TYPE, paymentRequestType);
        starter.putExtra(EXTRA_REQUEST_CODE, requestCode);
        starter.putExtra(EXTRA_ACTIVITY_TITLE, title);
        fragment.startActivityForResult(starter, requestCode);
    }

    @Override
    protected AccountChooserPresenter createPresenter() {
        return accountChooserPresenter;
    }

    @Override
    protected AccountChooserView getView() {
        return this;
    }

}
