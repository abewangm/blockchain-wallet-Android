package piuk.blockchain.android.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_credentials.*
import kotlinx.android.synthetic.main.include_entropy_meter.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.pairing.PairOrCreateWalletActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.annotations.Thunk
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible

class CredentialsFragment : BaseFragment<CredentialsView, CredentialsPresenter>(), CredentialsView, TextWatcher, View.OnFocusChangeListener {

    @Thunk internal val strengthVerdicts = intArrayOf(R.string.strength_weak, R.string.strength_medium, R.string.strength_normal, R.string.strength_strong)
    @Thunk internal val strengthColors = intArrayOf(R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_blue, R.drawable.progress_green)

    companion object {
        const val KEY_INTENT_EMAIL = "intent_email"
        const val KEY_INTENT_PASSWORD = "intent_password"
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = container!!.inflate(R.layout.fragment_credentials)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.parseExtras(activity.intent)

        tos.movementMethod.apply { LinkMovementMethod.getInstance() } //make link clickable
        command_next.isClickable = false
        entropy_container.pass_strength_bar.max = 100

        wallet_pass.setOnFocusChangeListener(this)
        wallet_pass.addTextChangedListener(this)

        command_next.setOnClickListener { onNextClicked() }

        val text = getString(R.string.agree_terms_of_service) + " "
        val text2 = getString(R.string.blockchain_tos)

        val spannable = SpannableString(text + text2)
        spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(activity, R.color.primary_blue_accent)),
                text.length, text.length + text2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tos.setText(spannable, TextView.BufferType.SPANNABLE)
        tos.setOnClickListener({
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse(SettingsFragment.URL_TOS_POLICY)))
        })

        hideEntropyContainer()

        onViewReady()
    }

    override fun createPresenter(): CredentialsPresenter = CredentialsPresenter()

    override fun getMvpView(): CredentialsView = this

    override fun setTitleText(text: Int) {
        (activity as PairOrCreateWalletActivity).setupToolbar(
                (activity as AppCompatActivity).supportActionBar, text)
    }

    override fun setNextText(text: Int) {
        command_next.setText(text)
    }

    fun hideEntropyContainer() {
        entropy_container.gone()
    }

    fun showEntropyContainer() {
        entropy_container.visible()
    }

    override fun afterTextChanged(editable: Editable?) {
        showEntropyContainer()
        presenter.calculateEntropy(editable.toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No-op
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // No-op
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            showEntropyContainer()
        } else {
            hideEntropyContainer()
        }
    }

    override fun setEntopyStrength(score: Int) {
        entropy_container.pass_strength_bar.setProgress(score)
    }


    override fun setEntopyLevel(level: Int) {
        entropy_container.pass_strength_verdict.setText(strengthVerdicts[level])
        entropy_container.pass_strength_bar.setProgressDrawable(
                ContextCompat.getDrawable(getActivity(), strengthColors[level]))
        entropy_container.pass_strength_verdict.setText(strengthVerdicts[level])
    }

    private fun onNextClicked() {

        val email = email_address.getText().toString().trim()
        val password1 = wallet_pass.getText().toString()
        val password2 = wallet_pass_confirm.getText().toString()

        presenter.validateCredentials(email, password1, password2)
    }

    private fun getNextActivityIntent(email: String, password: String): Intent {
        val intent = Intent(activity, getNextActivity())
        intent.putExtra(KEY_INTENT_EMAIL, email)
        intent.putExtra(KEY_INTENT_PASSWORD, password)
        intent.putExtra(LandingActivity.KEY_INTENT_RECOVERING_FUNDS, presenter.recoveringFunds)
        return intent
    }

    private fun getNextActivity(): Class<*> {
        return if (presenter.recoveringFunds) RecoverFundsActivity::class.java else PinEntryActivity::class.java
    }

    override fun showToast(message: Int, toastType: String) {
        context.toast(message, toastType)
    }

    override fun showWeakPasswordDialog(email: String, password: String) {
        val builder = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.weak_password)
                .setPositiveButton(android.R.string.yes, { dialog, whichButton ->
                    wallet_pass.setText("")
                    wallet_pass_confirm.setText("")
                    wallet_pass.requestFocus()
                })
                .setNegativeButton(android.R.string.no, { dialog, whichButton ->
                    startNextActivity(email, password)
                })

        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    override fun startNextActivity(email: String, password: String) {
        ViewUtils.hideKeyboard(activity)
        activity.startActivity(getNextActivityIntent(email, password))
    }
}