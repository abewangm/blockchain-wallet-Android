package piuk.blockchain.android.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog;
import piuk.blockchain.android.ui.fingerprint.FingerprintStage;
import piuk.blockchain.android.ui.home.MainActivity;

public class OnboardingActivity extends BaseMvpActivity<OnboardingView, OnboardingPresenter>
        implements OnboardingView,
        FingerprintPromptFragment.OnFragmentInteractionListener,
        EmailPromptFragment.OnFragmentInteractionListener {

    private static final int EMAIL_CLIENT_REQUEST = 5400;

    /**
     * Flag for showing only the email verification prompt. This is for use when signup was
     * completed some other time, but the user hasn't verified their email yet.
     */
    public static final String EXTRAS_EMAIL_ONLY = "email_only";

    @Inject OnboardingPresenter onboardingPresenter;
    private boolean emailLaunched = false;
    private MaterialProgressDialog progressDialog;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        progressDialog = new MaterialProgressDialog(this);
        progressDialog.setMessage(R.string.please_wait);
        progressDialog.setCancelable(false);
        progressDialog.show();

        onViewReady();
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
        if (!isFinishing()) {
            dismissDialog();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, FingerprintPromptFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void showEmailPrompt() {
        if (!isFinishing()) {
            dismissDialog();
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, EmailPromptFragment.newInstance(getPresenter().getEmail()))
                    .commit();
        }
    }

    @Override
    public void onEnableFingerprintClicked() {
        getPresenter().onEnableFingerprintClicked();
    }

    @Override
    public void onCompleteLaterClicked() {
        showEmailPrompt();
    }

    @Override
    public void showFingerprintDialog(String pincode) {
        if (!isFinishing()) {
            FingerprintDialog dialog = FingerprintDialog.Companion.newInstance(pincode, FingerprintStage.REGISTER_FINGERPRINT);
            //noinspection AnonymousInnerClassMayBeStatic
            dialog.setAuthCallback(new FingerprintDialog.FingerprintAuthCallback() {
                @Override
                public void onAuthenticated(String data) {
                    dialog.dismissAllowingStateLoss();
                    getPresenter().setFingerprintUnlockEnabled(true);
                    showEmailPrompt();
                }

                @Override
                public void onCanceled() {
                    dialog.dismissAllowingStateLoss();
                    getPresenter().setFingerprintUnlockEnabled(true);
                }
            });

            dialog.show(getSupportFragmentManager(), FingerprintDialog.TAG);
        }
    }

    @Override
    public void showEnrollFingerprintsDialog() {
        if (!isFinishing()) {
            new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.fingerprint_no_fingerprints_added)
                    .setCancelable(true)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS), 0))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onVerifyEmailClicked() {
        AccessState.getInstance().disableAutoLogout();
        emailLaunched = true;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        startActivity(Intent.createChooser(intent, getString(R.string.security_centre_email_check)));
    }

    @Override
    public void onVerifyLaterClicked() {
        startMainActivity();
    }

    @Override
    protected OnboardingPresenter createPresenter() {
        return onboardingPresenter;
    }

    @Override
    protected OnboardingView getView() {
        return this;
    }

    private void startMainActivity() {
        dismissDialog();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, EMAIL_CLIENT_REQUEST);
    }

    private void dismissDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EMAIL_CLIENT_REQUEST) {
            AccessState.getInstance().enableAutoLogout();
        }
    }
}
