package piuk.blockchain.android.ui.contacts;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityContactDetailBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class ContactDetailActivity extends BaseAuthActivity {

    ActivityContactDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_detail);

        setSupportActionBar(binding.toolbar.toolbarGeneral);
        binding.toolbar.toolbarGeneral.setTitle("someone's name i guess");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
}
