package piuk.blockchain.android.ui.upgrade;

import android.support.annotation.StringRes;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface UpgradeWalletView extends View {

    void showChangePasswordDialog();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void onUpgradeStarted();

    void onUpgradeCompleted();

    void onUpgradeFailed();

    void onBackButtonPressed();

    void showProgressDialog(@StringRes int message);

    void dismissProgressDialog();
}
