package piuk.blockchain.android.ui.fingerprint

import android.annotation.TargetApi
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatDialogFragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import piuk.blockchain.android.R
import piuk.blockchain.android.util.annotations.Thunk

const val TAG = "FingerprintDialog"
const val KEY_BUNDLE_PIN_CODE = "pin_code"
const val KEY_BUNDLE_STAGE = "stage"
private const val ERROR_TIMEOUT_MILLIS: Long = 1500
private const val SUCCESS_DELAY_MILLIS: Long = 600
private const val FATAL_ERROR_TIMEOUT_MILLIS: Long = 3500

class FingerprintDialog : AppCompatDialogFragment(), FingerprintDialogViewModel.DataListener {

    @Thunk lateinit var fingerprintIcon: ImageView
    @Thunk lateinit var statusTextView: TextView
    private lateinit var viewModel: FingerprintDialogViewModel
    private lateinit var descriptionTextView: TextView
    private lateinit var cancelButton: Button
    private var authCallback: FingerprintAuthCallback? = null

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (authCallback == null) {
            throw RuntimeException("Auth Callback is null, have you passed in into the dialog via setAuthCallback?")
        }
        setStyle(AppCompatDialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.apply {
            setTitle(getString(R.string.fingerprint_login_title))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener(BackButtonListener(authCallback!!))
        }

        viewModel = FingerprintDialogViewModel(this)

        val view = inflater!!.inflate(R.layout.dialog_fingerprint, container, false)

        fingerprintIcon = view.findViewById(R.id.icon_fingerprint) as ImageView
        descriptionTextView = view.findViewById(R.id.fingerprint_description) as TextView
        statusTextView = view.findViewById(R.id.fingerprint_status) as TextView
        cancelButton = view.findViewById(R.id.action_cancel) as Button
        cancelButton.setOnClickListener { _ -> authCallback!!.onCanceled() }

        viewModel.onViewReady()

        return view
    }

    fun setAuthCallback(authCallback: FingerprintAuthCallback) {
        this.authCallback = authCallback
    }

    override fun setCancelButtonText(@StringRes text: Int) {
        cancelButton.setText(text)
    }

    override fun setDescriptionText(@StringRes text: Int) {
        descriptionTextView.setText(text)
    }

    override fun setStatusText(@StringRes text: Int) {
        statusTextView.setText(text)
    }

    override fun setStatusText(text: String) {
        statusTextView.text = text
    }

    override fun setStatusTextColor(@ColorRes color: Int) {
        statusTextView.setTextColor(ContextCompat.getColor(context, color))
    }

    override fun setIcon(@DrawableRes drawable: Int) {
        fingerprintIcon.setImageResource(drawable)
    }

    override fun getBundle(): Bundle = arguments

    override fun onAuthenticated(data: String?) {
        statusTextView.removeCallbacks(resetErrorTextRunnable)
        fingerprintIcon.postDelayed({ authCallback!!.onAuthenticated(data) }, SUCCESS_DELAY_MILLIS)
    }

    override fun onRecoverableError() {
        showErrorAnimation(ERROR_TIMEOUT_MILLIS)
    }

    override fun onFatalError() {
        showErrorAnimation(FATAL_ERROR_TIMEOUT_MILLIS)
        fingerprintIcon.postDelayed({ authCallback!!.onCanceled() }, FATAL_ERROR_TIMEOUT_MILLIS)
    }

    override fun onCanceled() {
        authCallback!!.onCanceled()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        viewModel.destroy()
        super.onDismiss(dialog)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        // This is to fix a long-standing bug in the Android framework
    }

    private val resetErrorTextRunnable = Runnable {
        if (context != null) {
            statusTextView.setTextColor(ContextCompat.getColor(context, R.color.primary_gray_medium))
            statusTextView.text = getString(R.string.fingerprint_hint)
            fingerprintIcon.setImageResource(R.drawable.ic_fingerprint_logo)
        }
    }

    private fun showErrorAnimation(timeout: Long) {
        val shake = AnimationUtils.loadAnimation(context, R.anim.fingerprint_failed_shake)
        fingerprintIcon.animation = shake
        fingerprintIcon.animate()
        statusTextView.removeCallbacks(resetErrorTextRunnable)
        statusTextView.postDelayed(resetErrorTextRunnable, timeout)
    }

    interface FingerprintAuthCallback {

        fun onAuthenticated(data: String?)

        fun onCanceled()

    }

    private class BackButtonListener constructor(
            private val fingerprintAuthCallback: FingerprintAuthCallback
    ) : DialogInterface.OnKeyListener {

        override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return true
                } else {
                    fingerprintAuthCallback.onCanceled()
                    return true
                }
            } else {
                return false
            }
        }
    }

    companion object {
        fun newInstance(pin: String, stage: FingerprintStage): FingerprintDialog {
            val fragment = FingerprintDialog()
            fragment.arguments = Bundle().apply {
                putString(KEY_BUNDLE_PIN_CODE, pin)
                putString(KEY_BUNDLE_STAGE, stage.name)
            }
            return fragment
        }
    }

}

/**
 * Indicate which stage of the auth process the user is currently at
 */
enum class FingerprintStage {
    REGISTER_FINGERPRINT,
    AUTHENTICATE
}
