package piuk.blockchain.android.ui.settings.customprefs

import android.content.Context
import android.graphics.Typeface
import android.support.v7.preference.SwitchPreferenceCompat
import android.util.AttributeSet
import piuk.blockchain.android.util.helperfunctions.CustomFont
import piuk.blockchain.android.util.helperfunctions.loadFont

@Suppress("unused")
class CustomSwitchPreferenceCompat : SwitchPreferenceCompat {

    private var typeface: Typeface? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
            context,
            attrs,
            defStyleAttr,
            defStyleRes
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    private fun init() {
        loadFont(context, CustomFont.MontserratRegular) { typeface = it }
        // Forces setting fonts when Summary or Title are set via XMl
        this.title = title
        this.summary = summary
    }

    override fun setTitle(titleResId: Int) {
        title = context.getString(titleResId)
    }

    override fun setTitle(title: CharSequence?) {
//        val charSequence = CalligraphyUtils.applyTypefaceSpan(
//                title,
//                typeface
//        )
        super.setTitle(title)
    }

    override fun setSummary(summaryResId: Int) {
        summary = context.getString(summaryResId)
    }

    override fun setSummary(summary: CharSequence?) {
//        val charSequence = CalligraphyUtils.applyTypefaceSpan(
//                summary,
//                typeface
//        )
        super.setSummary(summary)
    }

}
