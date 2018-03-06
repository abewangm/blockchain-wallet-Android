package piuk.blockchain.android.util.helperfunctions

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.provider.FontRequest
import android.support.v4.provider.FontsContractCompat
import piuk.blockchain.android.R
import timber.log.Timber

/**
 * Loads a font via the AppCompat downloadable font system and returns a [Typeface] via a function
 * if present. If this call fails, it will do so silently and never return.
 *
 * @param context The current [Context]
 * @param font A [CustomFont] object that encapsulates the query to be sent to the fonts provider
 * @param func A function that accepts a [Typeface], used to return the loaded [Typeface] object
 */
fun loadFont(context: Context, font: CustomFont, func: (Typeface) -> Unit) {
    val handlerThread = HandlerThread("fonts")
    handlerThread.start()
    val handler = Handler(handlerThread.looper)

    val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            font.query,
            R.array.com_google_android_gms_fonts_certs
    )

    FontsContractCompat.requestFont(
            context,
            request,
            object : FontsContractCompat.FontRequestCallback() {

                override fun onTypefaceRetrieved(typeface: Typeface) {
                    func.invoke(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    Timber.e("FontsContractCompat.requestFont failed with error code $reason")
                }
            },
            handler
    )
}

enum class CustomFont(val query: String) {

    MONTSERRAT_REGULAR("Montserrat"),
    MONTSERRAT_LIGHT("name=Montserrat&weight=300"),
    MONTSERRAT_SEMI_BOLD("name=Montserrat&weight=600"),

}