package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactsInvitationBuilderBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class ContactsInvitationBuilderActivity extends BaseAuthActivity
        implements ContactsInvitationBuilderFragment1.FragmentInteractionListener,
        ContactsInvitationBuilderFragment2.FragmentInteractionListener,
        ContactsInvitationBuilderShareMethod.FragmentInteractionListener {

    private ActivityContactsInvitationBuilderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts_invitation_builder);

        binding.toolbar.toolbarGeneral.setTitle(R.string.contacts_add_contact_title);
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        submitFragmentTransaction(new ContactsInvitationBuilderFragment1());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, ContactsInvitationBuilderActivity.class);
        context.startActivity(starter);
    }

    @Override
    public void onInviteeNameSubmitted(String name) {
        // STOPSHIP: 10/01/2017 This is temporary
        Toast.makeText(this, "name", Toast.LENGTH_SHORT).show();

        submitFragmentTransaction(ContactsInvitationBuilderFragment2.newInstance(name));
    }

    @Override
    public void onMyNameSubmitted(String name) {
        // STOPSHIP: 10/01/2017 This is temporary
        Toast.makeText(this, "name", Toast.LENGTH_SHORT).show();

        submitFragmentTransaction(new ContactsInvitationBuilderShareMethod());
    }

    @Override
    public void onQrCodeSelected() {

    }

    @Override
    public void onLinkSelected() {

    }

    @Override
    public void onDoneSelected() {
        finish();
    }

    private void submitFragmentTransaction(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction
                .replace(R.id.content_frame, fragment)
                .commit();
    }
}
