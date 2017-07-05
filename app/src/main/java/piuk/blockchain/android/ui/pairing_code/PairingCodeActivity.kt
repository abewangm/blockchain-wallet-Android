package piuk.blockchain.android.ui.pairing_code

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import kotlinx.android.synthetic.main.activity_pairing_code.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.toast
import piuk.blockchain.android.util.extensions.visible
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent



class PairingCodeActivity : BaseMvpActivity<PairingCodeView, PairingCodePresenter>(), PairingCodeView {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing_code)

        val toolbar = findViewById(R.id.toolbar_general) as Toolbar
        setupToolbar(toolbar, R.string.pairing_code_log_in)

        pairing_first_step.setText(presenter.firstStep)

        onViewReady()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onQrLoaded(bitmap: Bitmap) {
        tv_warning.setText(R.string.pairing_code_warning_2)
        iv_qr.visible()

        val width = resources.displayMetrics.widthPixels
        val height = width * bitmap.height / bitmap.width
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        iv_qr.setImageBitmap(scaledBitmap)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun showProgressSpinner() {
        progress_bar.visible()
    }

    override fun hideProgressSpinner() {
        progress_bar.gone()
    }

    override fun createPresenter(): PairingCodePresenter = PairingCodePresenter()

    override fun getView(): PairingCodeView = this

    fun onClickQRToggle(view: View) {

        if (main_layout.visibility == View.VISIBLE) {

            //Show pairing QR
            main_layout.gone()
            btn_qr_toggle.setText(R.string.pairing_code_hide_qr)
            qr_layout.visible()
            iv_qr.gone()

            presenter.generatePairingQr()
        } else {

            //Hide pairing QR
            tv_warning.setText(R.string.pairing_code_warning_1)
            main_layout.visible()
            btn_qr_toggle.setText(R.string.pairing_code_show_qr)
            qr_layout.gone()
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, PairingCodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}