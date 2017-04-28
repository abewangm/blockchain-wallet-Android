package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.MotionEvent;

import info.blockchain.wallet.api.data.Settings;

import org.json.JSONObject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.ViewUtils;

/**
 * Created by adambennett on 09/08/2016.
 */

public class PasswordRequiredActivity extends BaseAuthActivity implements PasswordRequiredViewModel.DataListener {

    private PasswordRequiredViewModel mViewModel;
    private ActivityPasswordRequiredBinding mBinding;
    private MaterialProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_password_required);
        mViewModel = new PasswordRequiredViewModel(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.confirm_password);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mBinding.buttonContinue.setOnClickListener(view -> mViewModel.onContinueClicked());
        mBinding.buttonForget.setOnClickListener(view -> mViewModel.onForgetWalletClicked());

        mViewModel.onViewReady();
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void restartPage() {
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public String getPassword() {
        return mBinding.fieldPassword.getText().toString();
    }

    @Override
    public void resetPasswordField() {
        if (!isFinishing()) mBinding.fieldPassword.setText("");
    }

    @Override
    public void goToPinPage() {
        startActivity(new Intent(this, PinEntryActivity.class));
    }

    @Override
    public void updateWaitingForAuthDialog(int secondsRemaining) {
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining);
        }
    }

    @Override
    public void showForgetWalletWarning(DialogButtonCallback callback) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.forget_wallet_warning)
                .setPositiveButton(R.string.forget_wallet, (dialogInterface, i) -> callback.onPositiveClicked())
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> callback.onNegativeClicked())
                .create()
                .show();
    }

    @Override
    public void showTwoFactorCodeNeededDialog(JSONObject responseObject, String sessionId, int authType, String password) {
        ViewUtils.hideKeyboard(this);

        AppCompatEditText editText = new AppCompatEditText(this);
        editText.setHint(R.string.two_factor_dialog_hint);
        int message;
        if (authType == Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR) {
            message = R.string.two_factor_dialog_message_authenticator;
            editText.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL);
            editText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        } else if (authType == Settings.AUTH_TYPE_SMS) {
            message = R.string.two_factor_dialog_message_sms;
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        } else {
            throw new IllegalArgumentException("Auth Type " + authType + " should not be passed to this function");
        }

        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.two_factor_dialog_title)
                .setMessage(message)
                .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        mViewModel.submitTwoFactorCode(responseObject, sessionId, password, editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void showProgressDialog(@StringRes int messageId, @Nullable String suffix, boolean cancellable) {
        dismissProgressDialog();
        mProgressDialog = new MaterialProgressDialog(this);
        mProgressDialog.setCancelable(cancellable);
        if (suffix != null) {
            mProgressDialog.setMessage(getString(messageId) + "\n\n" + suffix);
        } else {
            mProgressDialog.setMessage(getString(messageId));
        }
        mProgressDialog.setOnCancelListener(dialogInterface -> mViewModel.onProgressCancelled());

        if (!isFinishing()) mProgressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.destroy();
        dismissProgressDialog();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user enters PIN
        return mViewModel.getAppUtil().detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }
}
