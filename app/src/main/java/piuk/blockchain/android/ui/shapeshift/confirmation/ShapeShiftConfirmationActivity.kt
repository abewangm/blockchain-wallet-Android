package piuk.blockchain.android.ui.shapeshift.confirmation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import timber.log.Timber

class ShapeShiftConfirmationActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_confirmation_shapeshift)
    }

    override fun onResume() {
        super.onResume()

        val shapeShiftData = intent.getParcelableExtra<ShapeShiftData>(EXTRA_SHAPESHIFT_DATA)
        Timber.d("DATAAAAA $shapeShiftData.toString()")
    }



    companion object {

        private const val EXTRA_SHAPESHIFT_DATA = "piuk.blockchain.android.EXTRA_SHAPESHIFT_DATA"

        @JvmStatic
        fun start(context: Context, shapeShiftData: ShapeShiftData) {
            val intent = Intent(context, ShapeShiftConfirmationActivity::class.java).apply {
                putExtra(EXTRA_SHAPESHIFT_DATA,  shapeShiftData)
            }

            context.startActivity(intent)
        }

    }
}