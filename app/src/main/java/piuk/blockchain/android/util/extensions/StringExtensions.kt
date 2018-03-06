package piuk.blockchain.android.util.extensions

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import piuk.blockchain.android.ui.settings.customprefs.CustomTypefaceSpan

/**
 * Contains Extension Functions related to [String] and [CharSequence] classes.
 */

/**
 * Applies a [Typeface] to a [CharSequence] and returns a [SpannableStringBuilder]
 *
 * @param typeface The font to be applied to the entire [CharSequence]. This is deliberate nullable,
 * and if null is passed the default system font will be applied instead.
 */
fun CharSequence.applyFont(typeface: Typeface?): SpannableStringBuilder =
        SpannableStringBuilder(this).apply {
            setSpan(
                    CustomTypefaceSpan(typeface),
                    0,
                    this.length,
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }