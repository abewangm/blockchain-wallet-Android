package piuk.blockchain.android.ui.login;

import android.content.Context;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import info.blockchain.wallet.api.data.Settings;

import org.json.JSONObject;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityManualPairingBinding;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.base.BaseMvpActivity;
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ViewUtils;

public class ManualPairingActivity extends BaseMvpActivity<ManualPairingView, ManualPairingPresenter>
        implements ManualPairingView {

    @Inject ManualPairingPresenter manualPairingPresenter;
    private MaterialProgressDialog mProgressDialog;
    private ActivityManualPairingBinding mBinding;

    {
        Injector.getInstance().getPresenterComponent().inject(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_manual_pairing);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.manual_pairing);

        mBinding.commandNext.setOnClickListener(v -> getPresenter().onContinueClicked());

        mBinding.walletPass.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                getPresenter().onContinueClicked();
            }
            return true;
        });

        onViewReady();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
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
    public void showTwoFactorCodeNeededDialog(JSONObject responseObject, String sessionId, int authType, String guid, String password) {
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
                        getPresenter().submitTwoFactorCode(responseObject, sessionId, guid, password, editText.getText().toString()))
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
        mProgressDialog.setOnCancelListener(dialogInterface -> getPresenter().onProgressCancelled());

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
    public void resetPasswordField() {
        if (!isFinishing()) mBinding.walletPass.setText("");
    }

    @Override
    public String getGuid() {
        return mBinding.walletId.getText().toString();
    }

    @Override
    public String getPassword() {
        return mBinding.walletPass.getText().toString();
    }

    @Override
    public void onDestroy() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Test for screen overlays before user enters PIN
        return getPresenter().getAppUtil().detectObscuredWindow(this, event) || super.dispatchTouchEvent(event);
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    protected ManualPairingPresenter createPresenter() {
        return manualPairingPresenter;
    }

    @Override
    protected ManualPairingView getView() {
        return this;
    }
}
