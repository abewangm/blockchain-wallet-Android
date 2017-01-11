package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactPairingMethodBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class ContactInviteActivity extends BaseAuthActivity {

    private ActivityContactPairingMethodBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_pairing_method);

        binding.toolbar.toolbarGeneral.setTitle(R.string.contacts_pairing_method_title);
        setSupportActionBar(binding.toolbar.toolbarGeneral);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonInvite.setOnClickListener(v -> ContactsInvitationBuilderActivity.start(this));

        binding.buttonAccept.setOnClickListener(
                v -> startActivityForResult(new Intent(this, ContactsAcceptInviteActivity.class), 437));
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
