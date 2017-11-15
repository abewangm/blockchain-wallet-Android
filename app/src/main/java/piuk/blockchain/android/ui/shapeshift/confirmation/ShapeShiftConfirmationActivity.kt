package piuk.blockchain.android.ui.shapeshift.confirmation

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData

class ShapeShiftConfirmationActivity : AppCompatActivity() {




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