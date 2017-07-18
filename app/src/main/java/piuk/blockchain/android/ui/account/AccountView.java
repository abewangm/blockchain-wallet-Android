package piuk.blockchain.android.ui.account;

import android.content.Intent;
import android.support.annotation.StringRes;

import info.blockchain.wallet.payload.data.LegacyAddress;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface AccountView extends View {

    void onShowTransferableLegacyFundsWarning(boolean isAutoPopup);

    void onSetTransferLegacyFundsMenuItemVisible(boolean visible);

    void showProgressDialog(@StringRes int message);

    void dismissProgressDialog();

    void onUpdateAccountsList();

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void broadcastIntent(Intent intent);

    void showWatchOnlyWarningDialog(String address);

    void showRenameImportedAddressDialog(LegacyAddress address);

    void startScanForResult();

    void showBip38PasswordDialog(String data);
}
