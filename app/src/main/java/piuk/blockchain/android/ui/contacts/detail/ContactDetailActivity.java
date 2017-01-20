package piuk.blockchain.android.ui.contacts.detail;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactDetailBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID;

public class ContactDetailActivity extends BaseAuthActivity implements
        ContactDetailFragment.OnFragmentInteractionListener {

    private ActivityContactDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_detail);

        binding.toolbar.toolbarGeneral.setTitle("");
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null && getIntent().hasExtra(KEY_BUNDLE_CONTACT_ID)) {
            submitFragmentTransaction(
                    ContactDetailFragment.newInstance(
                            getIntent().getStringExtra(KEY_BUNDLE_CONTACT_ID)));
        } else {
            finish();
        }
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
    public void onPaymentInitiated(String uri, String contactId) {

    }
}
