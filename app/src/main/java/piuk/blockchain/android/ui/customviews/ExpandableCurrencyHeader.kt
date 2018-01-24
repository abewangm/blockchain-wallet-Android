package piuk.blockchain.android.ui.customviews

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import kotlinx.android.synthetic.main.view_expanding_currency_header.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.setAnimationListener


class ExpandableCurrencyHeader @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), View.OnClickListener {

    private lateinit var selectionListener: (CryptoCurrencies) -> Unit
    var selectedCurrency = CryptoCurrencies.BTC

    init {
        // Inflate layout
        LayoutInflater.from(getContext())
                .inflate(R.layout.view_expanding_currency_header, this, true)
        // Set click listeners
        textview_selected_currency.setOnClickListener(this)
        textview_bitcoin.setOnClickListener(this)
        textview_ethereum.setOnClickListener(this)
        textview_bitcoin_cash.setOnClickListener(this)
        // Add compound drawables manually to avoid inflation errors on <21
        textview_bitcoin.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context, R.drawable.vector_bitcoin),
                null,
                null,
                null
        )
        textview_ethereum.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context, R.drawable.vector_eth),
                null,
                null,
                null
        )
        textview_bitcoin_cash.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context, R.drawable.vector_bitcoin_cash),
                null,
                null,
                null
        )
        // Select Bitcoin by default but without triggering callback
        updateCurrencyUi(R.drawable.vector_bitcoin, R.string.bitcoin)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = CustomOutline(w, h)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.textview_selected_currency -> openLayout()
            R.id.textview_bitcoin -> closeLayout(CryptoCurrencies.BTC)
            R.id.textview_ethereum -> closeLayout(CryptoCurrencies.ETHER)
            R.id.textview_bitcoin_cash -> closeLayout(CryptoCurrencies.BCH)
        }
    }

    fun setSelectionListener(selectionListener: (CryptoCurrencies) -> Unit) {
        this.selectionListener = selectionListener
    }

    fun setCurrentlySelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        // Prevent selecting the same thing twice needlessly
        if (selectedCurrency != cryptoCurrency) {
            selectedCurrency = cryptoCurrency
            when (selectedCurrency) {
                CryptoCurrencies.BTC ->
                    updateCurrencyUi(R.drawable.vector_bitcoin, R.string.bitcoin)
                CryptoCurrencies.ETHER ->
                    updateCurrencyUi(R.drawable.vector_eth, R.string.ethereum)
                CryptoCurrencies.BCH ->
                    updateCurrencyUi(R.drawable.vector_bitcoin_cash, R.string.bitcoin_cash)
            }
        }
    }

    fun hideEthereum() {
        textview_ethereum.gone()
    }

    private fun updateCurrencyUi(@DrawableRes leftDrawable: Int, @StringRes title: Int) {
        textview_selected_currency.run {
            text = context.getText(title).toString().toUpperCase()
            setCompoundDrawablesWithIntrinsicBounds(
                    AppCompatResources.getDrawable(context, leftDrawable),
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_arrow_drop_down_grey600_24dp),
                    null
            )
        }
    }

    private fun openLayout() {
        val animation = AlphaAnimation(1.0f, 0.0f).apply { duration = 250 }
        textview_selected_currency.startAnimation(animation)
        animation.setAnimationListener {
            onAnimationEnd {
                textview_selected_currency.alpha = 0.0f
                animateCoinSelectionLayout(View.VISIBLE)
            }
        }
    }

    private fun closeLayout(cryptoCurrency: CryptoCurrencies) {
        // Inform parent of currency selection
        selectionListener(cryptoCurrency)
        // Update UI
        setCurrentlySelectedCurrency(cryptoCurrency)
        // Trigger layout change
        animateCoinSelectionLayout(View.GONE)
        // Fade in title
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }
        textview_selected_currency.startAnimation(alphaAnimation)
        alphaAnimation.setAnimationListener {
            onAnimationEnd { textview_selected_currency.alpha = 1.0f }
        }
    }

    private fun animateCoinSelectionLayout(@ViewUtils.Visibility visibility: Int) {
        val elevation = if (AndroidUtils.is21orHigher()) {
            this.elevation
        } else {
            0.0f
        }

        ConstraintSet().apply {
            layoutAnimationListener = object: Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) = Unit

                override fun onAnimationEnd(animation: Animation?) {
                    if (AndroidUtils.is21orHigher()) {
                        // Restore elevation
                        this@ExpandableCurrencyHeader.elevation = elevation
                    }
                }

                override fun onAnimationStart(animation: Animation?) {
                    // Temporarily remove elevation as it interferes with animation
                    if (AndroidUtils.is21orHigher()) {
                        this@ExpandableCurrencyHeader.elevation = 0.0f
                    }
                }
            }
            clone(constraint_layout)
            setVisibility(R.id.linear_layout_coin_selection, visibility)
            applyTo(constraint_layout)
        }

        TransitionManager.beginDelayedTransition(
                constraint_layout,
                ChangeBounds().apply { duration = 300 }
        )
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class CustomOutline internal constructor(
            internal var width: Int,
            internal var height: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRect(0, 0, width, height)
        }

    }

}