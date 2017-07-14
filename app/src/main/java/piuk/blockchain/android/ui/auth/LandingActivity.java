package piuk.blockchain.android.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.databinding.ActivityLandingBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.login.LoginActivity;
import piuk.blockchain.android.ui.recover.RecoverFundsActivity;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;


public class LandingActivity extends BaseAuthActivity {

    private AppUtil appUtil;

    public static void start(Context context) {
        Intent intent = new Intent(context, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityLandingBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_landing);
        appUtil = new AppUtil(this);

        EnvironmentSettings environmentSettings = new EnvironmentSettings();

        if (environmentSettings.shouldShowDebugMenu()) {
            ToastCustom.makeText(
                    this,
                    "Current environment: "
                            + environmentSettings.getEnvironment().getName(),
                    ToastCustom.LENGTH_SHORT,
                    ToastCustom.TYPE_GENERAL);

            binding.buttonSettings.setVisibility(View.VISIBLE);
            binding.buttonSettings.setOnClickListener(view ->
                    new EnvironmentSwitcher(this, new PrefsUtil(this)).showDebugMenu());
        }

        binding.create.setOnClickListener(view -> startCreateActivity());
        binding.login.setOnClickListener(view -> startLoginActivity());
        binding.recoverFunds.setOnClickListener(view -> showFundRecoveryWarning());

        if (!ConnectivityStatus.hasConnectivity(this)) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setMessage(getString(R.string.check_connectivity_exit))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue, (d, id) -> {
                        Intent intent = new Intent(LandingActivity.this, LandingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    })
                    .create()
                    .show();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void startCreateActivity() {
        Intent intent = new Intent(this, CreateWalletActivity.class);
        startActivity(intent);
    }

    private void startRecoveryActivityFlow() {
        Intent intent = new Intent(this, RecoverFundsActivity.class);
        startActivity(intent);
    }

    private void showFundRecoveryWarning() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.recover_funds_warning_message)
                .setPositiveButton(R.string.dialog_continue, (dialogInterface, i) -> startRecoveryActivityFlow())
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void startLogoutTimer() {
        // No-op
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user creates a new wallet or enters confidential information
        return appUtil.detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }
}
