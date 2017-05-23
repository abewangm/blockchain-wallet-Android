package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialogKt;
import piuk.blockchain.android.ui.fingerprint.FingerprintStage;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.annotations.Thunk;

public class OnboardingActivity extends BaseAuthActivity implements OnboardingViewModel.DataListener,
        FingerprintPromptFragment.OnFragmentInteractionListener,
        EmailPromptFragment.OnFragmentInteractionListener {

    /**
     * Flag for showing only the email verification prompt. This is for use when signup was
     * completed some other time, but the user hasn't verified their email yet.
     */
    public static final String EXTRAS_EMAIL_ONLY = "email_only";

    @Thunk OnboardingViewModel viewModel;
    private boolean emailLaunched = false;
    private MaterialProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        viewModel = new OnboardingViewModel(this);

        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setMessage(R.string.please_wait);
        progressDialog.setCancelable(false);
        progressDialog.show();

        viewModel.onViewReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (emailLaunched) {
            startMainActivity();
        }
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public void showFingerprintPrompt() {
        dismissDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, FingerprintPromptFragment.newInstance())
                .commit();
    }

    @Override
    public void showEmailPrompt() {
        dismissDialog();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, EmailPromptFragment.newInstance(viewModel.getEmail()))
                .commit();
    }

    @Override
    public void onEnableFingerprintClicked() {
        viewModel.onEnableFingerprintClicked();
    }

    @Override
    public void onCompleteLaterClicked() {
        showEmailPrompt();
    }

    @Override
    public void showFingerprintDialog(String pincode) {
        FingerprintDialog dialog = FingerprintDialog.Companion.newInstance(pincode, FingerprintStage.REGISTER_FINGERPRINT);
        //noinspection AnonymousInnerClassMayBeStatic
        dialog.setAuthCallback(new FingerprintDialog.FingerprintAuthCallback() {
            @Override
            public void onAuthenticated(String data) {
                dialog.dismissAllowingStateLoss();
                viewModel.setFingerprintUnlockEnabled(true);
                showEmailPrompt();
            }

            @Override
            public void onCanceled() {
                dialog.dismissAllowingStateLoss();
                viewModel.setFingerprintUnlockEnabled(true);
            }
        });

        dialog.show(getSupportFragmentManager(), FingerprintDialogKt.TAG);
    }

    @Override
    public void showEnrollFingerprintsDialog() {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.fingerprint_no_fingerprints_added)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, which) ->
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS), 0))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onVerifyEmailClicked() {
        emailLaunched = true;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)));
    }

    @Override
    public void onVerifyLaterClicked() {
        startMainActivity();
    }

    private void startMainActivity() {
        dismissDialog();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void dismissDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
