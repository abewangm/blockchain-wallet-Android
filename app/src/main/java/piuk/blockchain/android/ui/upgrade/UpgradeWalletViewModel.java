package piuk.blockchain.android.ui.upgrade;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import info.blockchain.wallet.util.PasswordUtil;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

public class UpgradeWalletViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected AccessState accessState;
    @Inject protected AuthDataManager authDataManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected StringUtils stringUtils;

    interface DataListener {

        void showChangePasswordDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onUpgradeStarted();

        void onUpgradeCompleted();

        void onUpgradeFailed();

        void onBackButtonPressed();

        void showProgressDialog(@StringRes int message);

        void dimissProgressDialog();
    }

    UpgradeWalletViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // Check password existence
        String tempPassword = payloadDataManager.getTempPassword();
        if (tempPassword == null) {
            dataListener.showToast(R.string.upgrade_fail_info, ToastCustom.TYPE_ERROR);
            appUtil.clearCredentialsAndRestart();
            return;
        }

        // Check password strength
        if (PasswordUtil.ddpw(tempPassword) || PasswordUtil.getStrength(tempPassword) < 50) {
            dataListener.showChangePasswordDialog();
        }
    }

    void submitPasswords(String firstPassword, String secondPassword) {
        if (firstPassword.length() < 4
                || firstPassword.length() > 255
                || secondPassword.length() < 4
                || secondPassword.length() > 255) {
            dataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
        } else {
            if (!firstPassword.equals(secondPassword)) {
                dataListener.showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR);
            } else {
                final String currentPassword = payloadDataManager.getTempPassword();
                payloadDataManager.setTempPassword(secondPassword);

                compositeDisposable.add(
                        authDataManager.createPin(currentPassword, accessState.getPIN())
                                .andThen(payloadDataManager.syncPayloadWithServer())
                                .doOnError(ignored -> payloadDataManager.setTempPassword(currentPassword))
                                .doOnSubscribe(ignored -> dataListener.showProgressDialog(R.string.please_wait))
                                .doAfterTerminate(() -> dataListener.dimissProgressDialog())
                                .subscribe(
                                        () -> dataListener.showToast(R.string.password_changed, ToastCustom.TYPE_OK),
                                        throwable -> {
                                            dataListener.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                                            dataListener.showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
                                        }));
            }
        }
    }

    void onUpgradeRequested(@Nullable String secondPassword) {
        compositeDisposable.add(
                payloadDataManager.upgradeV2toV3(
                        secondPassword,
                        stringUtils.getString(R.string.default_wallet_name))
                        .doOnSubscribe(ignored -> dataListener.onUpgradeStarted())
                        .doOnError(ignored -> appUtil.setNewlyCreated(false))
                        .doOnComplete(() -> appUtil.setNewlyCreated(true))
                        .subscribe(
                                () -> dataListener.onUpgradeCompleted(),
                                throwable -> dataListener.onUpgradeFailed()
                        ));

    }

    void onContinueClicked() {
        prefs.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
        accessState.setIsLoggedIn(true);
        appUtil.restartAppWithVerifiedPin();
    }

    void onBackButtonPressed(Context context) {
        accessState.logout(context);
        dataListener.onBackButtonPressed();
    }
}
