package piuk.blockchain.android.ui.fingerprint

import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.annotation.VisibleForTesting
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseViewModel
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog.Companion.KEY_BUNDLE_PIN_CODE
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog.Companion.KEY_BUNDLE_STAGE
import piuk.blockchain.android.util.PrefsUtil
import javax.inject.Inject

class FingerprintDialogViewModel internal constructor(
        private val dataListener: FingerprintDialogViewModel.DataListener?
) : BaseViewModel(), FingerprintHelper.AuthCallback {

    init {
        Injector.getInstance().dataManagerComponent.inject(this)
    }

    @VisibleForTesting var currentStage: FingerprintStage? = null
    @VisibleForTesting var pincode: String? = null
    @Inject lateinit var fingerprintHelper: FingerprintHelper

    interface DataListener {

        fun getBundle(): Bundle

        fun setCancelButtonText(@StringRes text: Int)

        fun setDescriptionText(@StringRes text: Int)

        fun setStatusText(@StringRes text: Int)

        fun setStatusText(text: String)

        fun setStatusTextColor(@ColorRes color: Int)

        fun setIcon(@DrawableRes drawable: Int)

        fun onFatalError()

        fun onAuthenticated(data: String?)

        fun onRecoverableError()

        fun onCanceled()

    }

    override fun onViewReady() {
        val stageString = dataListener?.getBundle()?.getString(KEY_BUNDLE_STAGE)
        pincode = dataListener?.getBundle()?.getString(KEY_BUNDLE_PIN_CODE)

        if (!stageString.isNullOrEmpty() && !pincode.isNullOrEmpty()) {
            currentStage = FingerprintStage.valueOf(stageString!!)
        } else {
            dataListener?.onCanceled()
            return
        }

        when (currentStage) {
            FingerprintStage.REGISTER_FINGERPRINT -> registerFingerprint()
            FingerprintStage.AUTHENTICATE -> authenticateFingerprint()
            else -> throw RuntimeException("Unknown stage passed to ViewModel")
        }
    }

    // Fingerprint not recognised
    override fun onFailure() {
        setFailureState(R.string.fingerprint_not_recognized, null)
        dataListener?.onRecoverableError()
    }

    // Some error occurred
    override fun onHelp(message: String) {
        setFailureState(null, null)
        dataListener?.setStatusText(message)
        dataListener?.onRecoverableError()
    }

    override fun onAuthenticated(data: String?) {
        dataListener?.setIcon(R.drawable.vector_fingerprint_success)
        dataListener?.setStatusTextColor(R.color.primary_blue_accent)
        dataListener?.setStatusText(R.string.fingerprint_success)
        dataListener?.onAuthenticated(data)

        if (currentStage == FingerprintStage.REGISTER_FINGERPRINT && data != null) {
            fingerprintHelper.storeEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, data)
        }
    }

    /**
     * Recently changed PIN on device or added another fingerprint, must re-register. Note this
     * won't ever be called when registering a fingerprint, see [FingerprintHelper.encryptString]
     */
    override fun onKeyInvalidated() {
        setFailureState(
                R.string.fingerprint_key_invalidated_brief,
                R.string.fingerprint_key_invalidated_description)
        dataListener?.setCancelButtonText(R.string.fingerprint_use_pin)
        dataListener?.onFatalError()

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        fingerprintHelper.setFingerprintUnlockEnabled(false)
    }

    // Too many attempts - show message appropriate to stage
    override fun onFatalError() {
        when (currentStage) {
            FingerprintStage.REGISTER_FINGERPRINT -> setFailureState(
                    R.string.fingerprint_fatal_error_brief,
                    R.string.fingerprint_fatal_error_register_description)
            else -> setFailureState(
                    R.string.fingerprint_fatal_error_brief,
                    R.string.fingerprint_fatal_error_authenticate_description)
        }
        dataListener?.onFatalError()

        fingerprintHelper.clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        fingerprintHelper.setFingerprintUnlockEnabled(false)
    }

    private fun setFailureState(@StringRes status: Int?, @StringRes description: Int?) {
        dataListener?.setIcon(R.drawable.vector_fingerprint_error)
        dataListener?.setStatusTextColor(R.color.product_red_medium)
        if (status != null) dataListener?.setStatusText(status)
        if (description != null) dataListener?.setDescriptionText(description)
    }

    private fun authenticateFingerprint() {
        dataListener?.setCancelButtonText(R.string.fingerprint_use_pin)
        fingerprintHelper.decryptString(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, pincode!!, this)
    }

    private fun registerFingerprint() {
        dataListener?.setCancelButtonText(android.R.string.cancel)
        dataListener?.setDescriptionText(R.string.fingerprint_prompt)
        fingerprintHelper.encryptString(PrefsUtil.KEY_ENCRYPTED_PIN_CODE, pincode!!, this)
    }

    override fun destroy() {
        super.destroy()
        fingerprintHelper.releaseFingerprintReader()
    }

}
