package piuk.blockchain.android.ui.upgrade;

import android.content.Context;
import android.support.annotation.Nullable;

import info.blockchain.wallet.util.PasswordUtil;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

public class UpgradeWalletPresenter extends BasePresenter<UpgradeWalletView> {

    private PrefsUtil prefs;
    private AppUtil appUtil;
    private AccessState accessState;
    private AuthDataManager authDataManager;
    private PayloadDataManager payloadDataManager;
    private StringUtils stringUtils;

    @Inject
    UpgradeWalletPresenter(PrefsUtil prefs,
                           AppUtil appUtil,
                           AccessState accessState,
                           AuthDataManager authDataManager,
                           PayloadDataManager payloadDataManager,
                           StringUtils stringUtils) {

        this.prefs = prefs;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.authDataManager = authDataManager;
        this.payloadDataManager = payloadDataManager;
        this.stringUtils = stringUtils;
    }

    @Override
    public void onViewReady() {
        // Check password existence
        String tempPassword = payloadDataManager.getTempPassword();
        if (tempPassword == null) {
            getView().showToast(R.string.upgrade_fail_info, ToastCustom.TYPE_ERROR);
            appUtil.clearCredentialsAndRestart();
            return;
        }

        // Check password strength
        if (PasswordUtil.ddpw(tempPassword) || PasswordUtil.getStrength(tempPassword) < 50) {
            getView().showChangePasswordDialog();
        }
    }

    void submitPasswords(String firstPassword, String secondPassword) {
        if (firstPassword.length() < 4
                || firstPassword.length() > 255
                || secondPassword.length() < 4
                || secondPassword.length() > 255) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
        } else {
            if (!firstPassword.equals(secondPassword)) {
                getView().showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR);
            } else {
                final String currentPassword = payloadDataManager.getTempPassword();
                payloadDataManager.setTempPassword(secondPassword);

                authDataManager.createPin(currentPassword, accessState.getPIN())
                        .andThen(payloadDataManager.syncPayloadWithServer())
                        .doOnError(ignored -> payloadDataManager.setTempPassword(currentPassword))
                        .doOnSubscribe(ignored -> getView().showProgressDialog(R.string.please_wait))
                        .doAfterTerminate(() -> getView().dismissProgressDialog())
                        .compose(RxUtil.addCompletableToCompositeDisposable(this))
                        .subscribe(
                                () -> getView().showToast(R.string.password_changed, ToastCustom.TYPE_OK),
                                throwable -> {
                                    getView().showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR);
                                    getView().showToast(R.string.password_unchanged, ToastCustom.TYPE_ERROR);
                                });
            }
        }
    }

    void onUpgradeRequested(@Nullable String secondPassword) {
        payloadDataManager.upgradeV2toV3(
                secondPassword,
                stringUtils.getString(R.string.default_wallet_name))
                .doOnSubscribe(ignored -> getView().onUpgradeStarted())
                .doOnError(ignored -> appUtil.setNewlyCreated(false))
                .doOnComplete(() -> appUtil.setNewlyCreated(true))
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        () -> getView().onUpgradeCompleted(),
                        throwable -> getView().onUpgradeFailed());

    }

    void onContinueClicked() {
        prefs.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
        accessState.setIsLoggedIn(true);
        appUtil.restartAppWithVerifiedPin();
    }

    void onBackButtonPressed(Context context) {
        accessState.logout(context);
        getView().onBackButtonPressed();
    }

}
