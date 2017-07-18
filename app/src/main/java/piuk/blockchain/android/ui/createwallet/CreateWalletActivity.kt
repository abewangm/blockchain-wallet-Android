package piuk.blockchain.android.ui.createwallet

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.RxTextView
import kotlinx.android.synthetic.main.activity_create_wallet.*
import kotlinx.android.synthetic.main.include_entropy_meter.view.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible
import piuk.blockchain.android.util.helperfunctions.consume
import javax.inject.Inject

class CreateWalletActivity : BaseMvpActivity<CreateWalletView, CreateWalletPresenter>(),
        CreateWalletView,
        View.OnFocusChangeListener {

    @Inject lateinit var createWalletPresenter: CreateWalletPresenter
    private var progressDialog: MaterialProgressDialog? = null

    private val strengthVerdicts = intArrayOf(
            R.string.strength_weak,
            R.string.strength_medium,
            R.string.strength_normal,
            R.string.strength_strong
    )
    private val strengthColors = intArrayOf(
            R.drawable.progress_red,
            R.drawable.progress_orange,
            R.drawable.progress_blue,
            R.drawable.progress_green
    )

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)

        presenter.parseExtras(intent)

        tos.movementMethod = LinkMovementMethod.getInstance() //make link clickable
        command_next.isClickable = false
        entropy_container.pass_strength_bar.max = 100 * 10

        wallet_pass.onFocusChangeListener = this
        RxTextView.afterTextChangeEvents(wallet_pass)
                .doOnNext({
                    showEntropyContainer()
                    presenter.calculateEntropy(it.toString())
                })
                .subscribe(IgnorableDefaultObserver())

        command_next.setOnClickListener { onNextClicked() }

        val text = getString(R.string.agree_terms_of_service) + " "
        val text2 = getString(R.string.blockchain_tos)

        val spannable = SpannableString(text + text2)
        spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_blue_accent)),
                text.length,
                text.length + text2.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tos.setText(spannable, TextView.BufferType.SPANNABLE)
        tos.setOnClickListener({
            startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(SettingsFragment.URL_TOS_POLICY))
            )
        })

        wallet_pass_confirm.setOnEditorActionListener({ _, i, _ ->
            consume { if (i == EditorInfo.IME_ACTION_GO) onNextClicked() }
        })

        hideEntropyContainer()

        onViewReady()
    }

    override fun getView() = this

    override fun createPresenter() = createWalletPresenter

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setTitleText(text: Int) {
        setupToolbar(toolbar_general, text)
    }

    override fun setNextText(text: Int) {
        command_next.setText(text)
    }

    private fun hideEntropyContainer() {
        entropy_container.gone()
    }

    private fun showEntropyContainer() {
        entropy_container.visible()
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) = when {
        hasFocus -> showEntropyContainer()
        else -> hideEntropyContainer()
    }

    override fun setEntropyStrength(score: Int) {
        ObjectAnimator.ofInt(
                entropy_container.pass_strength_bar,
                "progress",
                entropy_container.pass_strength_bar.progress,
                score * 10
        ).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun setEntropyLevel(level: Int) {
        entropy_container.pass_strength_verdict.setText(strengthVerdicts[level])
        entropy_container.pass_strength_bar.progressDrawable =
                ContextCompat.getDrawable(this, strengthColors[level])
        entropy_container.pass_strength_verdict.setText(strengthVerdicts[level])
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun showWeakPasswordDialog(email: String, password: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.weak_password)
                .setPositiveButton(android.R.string.yes, { _, _ ->
                    wallet_pass.setText("")
                    wallet_pass_confirm.setText("")
                    wallet_pass.requestFocus()
                })
                .setNegativeButton(android.R.string.no, { _, _ ->
                    presenter.createWallet(email, password)
                }).show()
    }

    override fun startPinEntryActivity() {
        ViewUtils.hideKeyboard(this)
        this.startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun showProgressDialog(message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(getString(message))
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun getDefaultAccountName(): String {
        return getString(R.string.default_wallet_name)
    }

    private fun onNextClicked() {
        val email = email_address.text.toString().trim()
        val password1 = wallet_pass.text.toString()
        val password2 = wallet_pass_confirm.text.toString()

        presenter.validateCredentials(email, password1, password2)
    }

}