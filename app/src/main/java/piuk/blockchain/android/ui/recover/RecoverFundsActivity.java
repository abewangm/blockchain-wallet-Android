package piuk.blockchain.android.ui.recover;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityRecoverFundsBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class RecoverFundsActivity extends BaseMvpActivity<RecoverFundsView, RecoverFundsPresenter>
        implements RecoverFundsView {

    public static final String RECOVERY_PHRASE = "RECOVERY_PHRASE";

    @Inject RecoverFundsPresenter recoverFundsPresenter;
    private ActivityRecoverFundsBinding binding;
    private MaterialProgressDialog materialProgressDialog;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_recover_funds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.recover_funds);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.buttonContinue.setOnClickListener(view -> getPresenter().onContinueClicked());
        binding.fieldPassphrase.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                getPresenter().onContinueClicked();
            }
            return true;
        });

        onViewReady();
    }

    @Override
    public void gotoCredentialsActivity(String recoveryPhrase) {
        Intent intent = new Intent(this, CreateWalletActivity.class);
        intent.putExtra(RECOVERY_PHRASE, recoveryPhrase);
        startActivity(intent);
    }

    @Override
    public String getRecoveryPhrase() {
        return binding.fieldPassphrase.getText().toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
    }

    @Override
    public void showProgressDialog(@StringRes int messageId) {
        dismissProgressDialog();
        materialProgressDialog = new MaterialProgressDialog(this);
        materialProgressDialog.setCancelable(false);
        materialProgressDialog.setMessage(getString(messageId));

        if (!isFinishing()) materialProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (materialProgressDialog != null && materialProgressDialog.isShowing()) {
            materialProgressDialog.dismiss();
            materialProgressDialog = null;
        }
    }

    @Override
    protected RecoverFundsPresenter createPresenter() {
        return recoverFundsPresenter;
    }

    @Override
    protected RecoverFundsView getView() {
        return this;
    }

}
