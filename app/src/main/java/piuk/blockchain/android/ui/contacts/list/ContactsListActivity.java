package piuk.blockchain.android.ui.contacts.list;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.text.InputType;
import android.view.View;
import android.widget.FrameLayout;

import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailActivity;
import piuk.blockchain.android.ui.contacts.pairing.ContactInviteActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.android.util.ViewUtils;

import static piuk.blockchain.android.ui.base.UiState.CONTENT;
import static piuk.blockchain.android.ui.base.UiState.EMPTY;
import static piuk.blockchain.android.ui.base.UiState.FAILURE;
import static piuk.blockchain.android.ui.base.UiState.LOADING;


public class ContactsListActivity extends BaseMvpActivity<ContactsListView, ContactsListPresenter>
        implements ContactsListView {

    public static final String EXTRA_METADATA_URI = "metadata_uri";
    public static final String KEY_BUNDLE_CONTACT_ID = "contact_id";

    private static final int REQUEST_PAIRING = 98;

    @Inject ContactsListPresenter contactsListPresenter;
    private ActivityContactsBinding binding;
    private ContactsListAdapter contactsListAdapter;
    private MaterialProgressDialog progressDialog;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);

        setupToolbar(binding.toolbar, R.string.contacts_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Buttons
        binding.fab.setOnClickListener(view -> ContactInviteActivity.start(this));
        binding.buttonRetry.setOnClickListener(view -> getPresenter().onViewReady());
        // Swipe to refresh layout
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_blue_accent);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> getPresenter().onViewReady());
        // Contacts list
        contactsListAdapter = new ContactsListAdapter(new ArrayList<>(), new StringUtils(this));
        contactsListAdapter.setContactsClickListener(id -> {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_BUNDLE_CONTACT_ID, id);
            ContactDetailActivity.start(this, bundle);
        });
        binding.recyclerviewContacts.setAdapter(contactsListAdapter);
        binding.recyclerviewContacts.setLayoutManager(new LinearLayoutManager(this));

        Typeface typeface = TypefaceUtils.load(getAssets(), "fonts/Montserrat-Regular.ttf");
        binding.toolbarLayout.setExpandedTitleTypeface(typeface);
        binding.toolbarLayout.setCollapsedTitleTypeface(typeface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onViewReady();
    }

    @Override
    public void onContactsLoaded(@NonNull List<ContactsListItem> contacts) {
        contactsListAdapter.onContactsUpdated(contacts);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    /**
     * Static method to assist with launching this activity
     */
    public static void start(Context context, @Nullable Bundle extras) {
        Intent starter = new Intent(context, ContactsListActivity.class);
        if (extras != null) starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PAIRING && resultCode == RESULT_OK) {
            getPresenter().onViewReady();
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void showSecondPasswordDialog() {
        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setHint(R.string.password);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        FrameLayout frameLayout = ViewUtils.getAlertDialogPaddedView(this, editText);

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.contacts_second_password_prompt)
                .setView(frameLayout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> getPresenter().initContactsService(editText.getText().toString()))
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
    public void setUiState(@UiState.UiStateDef int uiState) {
        switch (uiState) {
            case LOADING:
                binding.swipeRefreshLayout.setRefreshing(true);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case CONTENT:
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case FAILURE:
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.layoutFailure.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case EMPTY:
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected ContactsListPresenter createPresenter() {
        return contactsListPresenter;
    }

    @Override
    protected ContactsListView getView() {
        return this;
    }

}
