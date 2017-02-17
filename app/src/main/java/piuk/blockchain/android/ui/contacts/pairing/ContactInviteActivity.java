package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactPairingMethodBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class ContactInviteActivity extends BaseAuthActivity {

    private static final int REQUEST_CODE_ACCEPT_INVITE = 437;
    private ActivityContactPairingMethodBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_pairing_method);

        setupToolbar(binding.toolbar.toolbarGeneral, R.string.contacts_pairing_method_title);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonInvite.setOnClickListener(v -> ContactsInvitationBuilderActivity.start(this));

        binding.buttonAccept.setOnClickListener(
                v -> startActivityForResult(new Intent(this, ContactsAcceptInviteActivity.class), REQUEST_CODE_ACCEPT_INVITE));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Close parent activity on result regardless of success
        finish();
    }

    /**
     * Static method to assist with launching this activity
     */
    public static void start(Context context) {
        Intent starter = new Intent(context, ContactInviteActivity.class);
        context.startActivity(starter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
