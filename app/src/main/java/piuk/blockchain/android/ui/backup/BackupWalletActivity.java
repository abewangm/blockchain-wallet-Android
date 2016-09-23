package piuk.blockchain.android.ui.backup;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class BackupWalletActivity extends BaseAuthActivity {

    public static final String BACKUP_DATE_KEY = "BACKUP_DATE_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_wallet);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.toolbar_general);
        toolbar.setTitle(getResources().getString(R.string.backup_wallet));
        setSupportActionBar(toolbar);

        if (isBackedUp()) {
            startFragment(BackupWalletCompletedFragment.newInstance(false), BackupWalletCompletedFragment.TAG);
        } else {
            startFragment(new BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG);
        }
    }

    private void startFragment(Fragment fragment, String tag) {
        getFragmentManager().beginTransaction()
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
                && PayloadManager.getInstance().getPayload().getHdWallet() != null
                && PayloadManager.getInstance().getPayload().getHdWallet().isMnemonicVerified();
    }
}