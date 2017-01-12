package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;


public class ContactsListActivity extends BaseAuthActivity implements ContactsListViewModel.DataListener {

    public static final String EXTRA_METADATA_URI = "metadata_uri";
    public static final String KEY_BUNDLE_ID = "bundle_id";

    private static final int REQUEST_PAIRING = 98;
    private ActivityContactsBinding binding;
    private ContactsListViewModel viewModel;
    private ContactsListAdapter contactsListAdapter;
    private MaterialProgressDialog materialProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        viewModel = new ContactsListViewModel(this);

        binding.toolbar.setTitle(R.string.contacts_title);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.fab.setOnClickListener(view -> ContactInviteActivity.start(this));

        binding.buttonRetry.setOnClickListener(view -> viewModel.onViewReady());

        contactsListAdapter = new ContactsListAdapter(new ArrayList<>());
        contactsListAdapter.setContactsClickListener(id -> {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_BUNDLE_ID, id);
            ContactDetailActivity.start(this, bundle);
        });
        binding.layoutContent.setAdapter(contactsListAdapter);
        binding.layoutContent.setLayoutManager(new LinearLayoutManager(this));

        viewModel.onViewReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.requestUpdatedList();
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
        // TODO: 11/01/2017 Changing the RecyclerView's visibility prevents it from displaying
        // - must find out why. Already tried: postDelayed, runOnUiThread, invalidate RecyclerView,
        // invalidate CoordinatorLayout ¯\_(ツ)_/¯
        switch (uiState) {
            case LOADING:
                binding.layoutLoading.setVisibility(View.VISIBLE);
//                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case CONTENT:
                binding.layoutLoading.setVisibility(View.GONE);
//                binding.layoutContent.setVisibility(View.VISIBLE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case FAILURE:
                binding.layoutLoading.setVisibility(View.GONE);
//                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case EMPTY:
                binding.layoutLoading.setVisibility(View.GONE);
//                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutFailure.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                break;
        }
    }
}
