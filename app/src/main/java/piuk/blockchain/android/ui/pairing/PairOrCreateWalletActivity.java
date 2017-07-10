package piuk.blockchain.android.ui.pairing;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.auth.CredentialsFragment;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.zxing.CaptureActivity;
import piuk.blockchain.android.util.AppUtil;

public class PairOrCreateWalletActivity extends BaseAuthActivity implements PairingViewModel.DataListener {

    public static final int PAIRING_QR = 2005;
    private PairingViewModel viewModel;
    private MaterialProgressDialog materialProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        viewModel = new PairingViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setSupportActionBar(toolbar);

        if (getIntent().getIntExtra("starting_fragment", 1) == 1) {
            Fragment fragment = new PairWalletFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        } else {
            CredentialsFragment fragmentv4 = new CredentialsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragmentv4)
                    .commit();
        }

        viewModel.onViewReady();
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        new AppUtil(this).restartApp();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PAIRING_QR) {
            if (data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
                viewModel.pairWithQR(data.getStringExtra(CaptureActivity.SCAN_RESULT));
            }
        }
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_LONG, toastType);
    }

    @Override
    public void startPinEntryActivity() {
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();
        materialProgressDialog = new MaterialProgressDialog(this);
        materialProgressDialog.setCancelable(false);
        materialProgressDialog.setMessage(getString(message));

        if (!isFinishing()) materialProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (materialProgressDialog != null && materialProgressDialog.isShowing()) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }
}
