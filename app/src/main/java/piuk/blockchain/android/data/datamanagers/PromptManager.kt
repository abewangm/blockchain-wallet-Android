package piuk.blockchain.android.data.datamanagers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.LandingActivity
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.home.SecurityPromptDialog
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.ui.settings.SettingsFragment.EXTRA_SHOW_ADD_EMAIL_DIALOG
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.RootUtil
import java.util.*

class PromptManager(
        private val prefsUtil: PrefsUtil,
        private val payloadDataManager: PayloadDataManager,
        private val transactionListDataManager: TransactionListDataManager
) {

    private val ONE_MONTH = 28 * 24 * 60 * 60 * 1000L

    fun getDefaultPrompts(context: Context): Observable<List<AlertDialog>> {
        val list = mutableListOf<AlertDialog>()

        if (isSurveyAllowed()) list.add(getSurveyDialog(context))
        if (isRooted()) list.add(getRootWarningDialog(context))

        return Observable.fromArray(list)
    }

    fun getCustomPrompts(context: Context, settings: Settings): Observable<List<AppCompatDialogFragment>> {
        val list = mutableListOf<AppCompatDialogFragment>()

        if (isBackedUpReminderAllowed()) list.add(getBackupCustomDialog(context))
        if (is2FAReminderAllowed(settings)) list.add(get2FACustomDialog(context))
        if (isVerifyEmailReminderAllowed(settings)) list.add(getVerifyEmailCustomDialog(context))

        return Observable.fromArray(list)
    }

    private fun isFirstRun(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0) == 0
    }

    private fun getAppVisitCount(): Int {
        return prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)
    }

    private fun isSurveyComplete(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_SURVEY_COMPLETED, false)
    }

    private fun getGuid(): String {
        return prefsUtil.getValue(PrefsUtil.KEY_GUID, "")
    }

    private fun getIfNeverPrompt2Fa(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, false)
    }

    private fun getTimeOfLastSecurityPrompt(): Long {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, 0L)
    }

    private fun storeTimeOfLastSecurityPrompt() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, System.currentTimeMillis())
    }

    fun neverPrompt2Fa() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, true)
    }

    private fun getIfNeverPromptBackup(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, false)
    }

    private fun setBackupCompleted() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, true)
    }

    private fun hasTransactions(): Boolean {
        return !transactionListDataManager.transactionList.isEmpty()
    }

    private fun isRooted(): Boolean {
        return RootUtil().isDeviceRooted && !prefsUtil.getValue("disable_root_warning", false)
    }

    private fun isVerifyEmailReminderAllowed(settings: Settings): Boolean {
        return !isFirstRun() && settings.isEmailVerified && !settings.email.isEmpty()
    }

    private fun isSurveyAllowed(): Boolean {
        if (!isSurveyComplete()) {
            var visitsToPageThisSession = getAppVisitCount()
            // Trigger first time coming back to transaction tab
            if (visitsToPageThisSession == 1) {
                // Don't show past June 30th
                val surveyCutoffDate = Calendar.getInstance()
                surveyCutoffDate.set(Calendar.YEAR, 2017)
                surveyCutoffDate.set(Calendar.MONTH, Calendar.JUNE)
                surveyCutoffDate.set(Calendar.DAY_OF_MONTH, 30)

                if (Calendar.getInstance().before(surveyCutoffDate)) {
                    prefsUtil.setValue(PrefsUtil.KEY_SURVEY_COMPLETED, true)
                    return true
                }
            } else {
                visitsToPageThisSession++
                prefsUtil.setValue(PrefsUtil.KEY_SURVEY_VISITS, visitsToPageThisSession)
            }
        }

        return false
    }

    private fun is2FAReminderAllowed(settings: Settings): Boolean {
        // On third visit onwards, prompt 2FA
        val isEnoughVisits = (!getIfNeverPrompt2Fa() && getAppVisitCount() >= 3)
        val isNeeded = !settings.isSmsVerified && settings.authType == Settings.AUTH_TYPE_OFF
        val isCorrectTime = getTimeOfLastSecurityPrompt() == 0L
                || System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH

        if (isEnoughVisits && isNeeded && isCorrectTime) {
            storeTimeOfLastSecurityPrompt()
        }

        return !isFirstRun() && isEnoughVisits && isNeeded && isCorrectTime
    }

    private fun isBackedUpReminderAllowed(): Boolean {
        val isAllowed = !isFirstRun()
                && !getIfNeverPromptBackup()
                && !payloadDataManager.isBackedUp && hasTransactions()

        val isCorrectTime = getTimeOfLastSecurityPrompt() == 0L
                || System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH

        if (isAllowed && isCorrectTime) {
            storeTimeOfLastSecurityPrompt()
            return true
        }

        return false
    }

    private fun isLastBackupReminder(): Boolean {
        return System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH
    }

    //********************************************************************************************//
    //*                              Default Prompts                                             *//
    //********************************************************************************************//

    private fun getRootWarningDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setMessage(R.string.device_rooted)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue, null)
                .create()
    }

    private fun getConnectivityDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setMessage(R.string.check_connectivity_exit)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue) { _, _ ->
                    if (getGuid().isEmpty()) {
                        LandingActivity.start(context)
                    } else {
                        PinEntryActivity.start(context)
                    }
                }.create()
    }

    private fun getSurveyDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.survey_message)
                .setPositiveButton(R.string.survey_positive_button) { _, _ ->
                    val url = "https://blockchain.co1.qualtrics.com/SE/?SID=SV_bQ8rW6DErUEzMeV"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                }
                .setNegativeButton(R.string.polite_no, null)
                .create()
    }

    //********************************************************************************************//
    //*                         Custom Security Prompts                                          *//
    //********************************************************************************************//

    private fun getVerifyEmailCustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_add_email_title,
                context.getString(R.string.security_centre_add_email_message),
                R.drawable.vector_email,
                R.string.security_centre_add_email_positive_button,
                true,
                false)
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            val bundle = Bundle()
            bundle.putBoolean(EXTRA_SHOW_ADD_EMAIL_DIALOG, true)
            SettingsActivity.start(context, bundle)
        }

        securityPromptDialog.setNegativeButtonListener { securityPromptDialog.dismiss() }

        return securityPromptDialog
    }

    private fun get2FACustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.two_fa,
                context.getString(R.string.security_centre_two_fa_message),
                R.drawable.vector_mobile,
                R.string.enable,
                true,
                true)
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                neverPrompt2Fa()
            }
            val bundle = Bundle()
            bundle.putBoolean(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true)
            SettingsActivity.start(context, bundle)
        }

        securityPromptDialog.setNegativeButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                neverPrompt2Fa()
            }
        }

        return securityPromptDialog
    }

    private fun getBackupCustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
                R.string.security_centre_backup_title,
                context.getString(R.string.security_centre_backup_message),
                R.drawable.vector_lock,
                R.string.security_centre_backup_positive_button,
                true,
                isLastBackupReminder())
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                setBackupCompleted()
            }
            BackupWalletActivity.start(context, null)
        }

        securityPromptDialog.setNegativeButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                setBackupCompleted()
            }
        }

        return securityPromptDialog
    }
}