package piuk.blockchain.android.ui.auth;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.ui.account.AccountPresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppRate;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

class EnvironmentSwitcher {

    private Context context;
    private PrefsUtil prefsUtil;

    EnvironmentSwitcher(Context context, PrefsUtil prefsUtil) {
        this.context = context;
        this.prefsUtil = prefsUtil;
    }

    void showDebugMenu() {
        new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle("Debug settings")
                .setMessage("Select 'Reset Timers' to reset various device timers and saved states, such as warning dialogs, onboarding etc.\n\nSelect 'Wipe Wallet' to log out and completely reset this app.")
                .setPositiveButton("Reset Timers", (dialogInterface, i) -> resetAllTimers())
                .setNegativeButton("Reset Wallet", (dialogInterface, i) ->
                        new AppUtil(context).clearCredentialsAndRestart())
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private void resetAllTimers() {
        prefsUtil.removeValue(PrefsUtil.KEY_PIN_FAILS);
        prefsUtil.removeValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED);
        prefsUtil.removeValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER);
        prefsUtil.removeValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER);
        prefsUtil.removeValue(AccountPresenter.KEY_WARN_TRANSFER_ALL);
        prefsUtil.removeValue(PrefsUtil.KEY_APP_VISITS);
        prefsUtil.removeValue(PrefsUtil.KEY_ONBOARDING_COMPLETE);
        prefsUtil.removeValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN);
        prefsUtil.removeValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED);

        AppRate.reset(context);
        AccessState.getInstance().setPIN(null);

        ToastCustom.makeText(context, "Timers reset", ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_OK);
    }

}
