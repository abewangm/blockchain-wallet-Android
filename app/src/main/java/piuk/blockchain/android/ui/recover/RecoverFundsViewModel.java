package piuk.blockchain.android.ui.recover;

import android.content.Intent;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;

public class RecoverFundsViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject AuthDataManager authDataManager;
    @Inject AppUtil appUtil;

    public interface DataListener {

        String getRecoveryPhrase();

        Intent getPageIntent();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog(@StringRes int messageId);

        void dismissProgressDialog();

        void goToPinEntryPage();

    }

    RecoverFundsViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        dataListener = listener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onContinueClicked() {
        String recoveryPhrase = dataListener.getRecoveryPhrase();
        if (recoveryPhrase == null || recoveryPhrase.isEmpty()) {
            dataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String trimmed = recoveryPhrase.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        if (words != 12) {
            dataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        String password = dataListener.getPageIntent().getStringExtra(KEY_INTENT_PASSWORD);

        if (password == null || password.isEmpty()) {
            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            appUtil.clearCredentialsAndRestart();
            return;
        }

        String email = dataListener.getPageIntent().getStringExtra(KEY_INTENT_EMAIL);

        if (email == null || email.isEmpty()) {
            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
            appUtil.clearCredentialsAndRestart();
            return;
        }

        authDataManager.restoreHdWallet(email, password, recoveryPhrase)
                .doOnSubscribe(ignored -> dataListener.showProgressDialog(R.string.creating_wallet))
                .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                .subscribe(
                        payload -> dataListener.goToPinEntryPage(),
                        throwable -> dataListener.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR));
    }

    public AppUtil getAppUtil() {
        return appUtil;
    }

}
