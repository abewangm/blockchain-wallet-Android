package piuk.blockchain.android.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.settings.SettingsActivity;

public class BackupWalletActivity extends BaseAuthActivity {

    public static final String BACKUP_DATE_KEY = "BACKUP_DATE_KEY";

    public static void start(Context context, @Nullable Bundle extras) {
        Intent starter = new Intent(context, BackupWalletActivity.class);
        if (extras != null) starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_wallet);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.backup_wallet);

        if (isBackedUp()) {
            startFragment(BackupWalletCompletedFragment.newInstance(false), BackupWalletCompletedFragment.TAG);
        } else {
            startFragment(new BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG);
        }
    }

    private void startFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean isBackedUp() {
        return PayloadManager.getInstance().getPayload() != null
                && PayloadManager.getInstance().getPayload().getHdWallets() != null
                && PayloadManager.getInstance().getPayload().getHdWallets().get(0).isMnemonicVerified();
    }
}