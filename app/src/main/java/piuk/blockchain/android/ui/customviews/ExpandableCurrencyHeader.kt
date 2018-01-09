package piuk.blockchain.android.ui.customviews

import android.animation.LayoutTransition
import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import kotlinx.android.synthetic.main.view_expanding_currency_header.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.util.ViewUtils
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
        // Set layout transition preferences
        constraint_layout.layoutTransition.apply {
            enableTransitionType(LayoutTransition.APPEARING)
            enableTransitionType(LayoutTransition.DISAPPEARING)
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
                CryptoCurrencies.BTC -> updateCurrencyUi(R.drawable.vector_bitcoin, R.string.bitcoin)
                CryptoCurrencies.ETHER -> updateCurrencyUi(R.drawable.vector_eth, R.string.ethereum)
                CryptoCurrencies.BCH -> updateCurrencyUi(R.drawable.vector_bitcoin_cash, R.string.bitcoin_cash)
            }
        }
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
        // Listen for finish of animation as UI responds to visibility change
        constraint_layout.layoutTransition.apply {
            addTransitionListener(object : LayoutTransition.TransitionListener {
                override fun startTransition(
                        transition: LayoutTransition?,
                        container: ViewGroup?,
                        view: View?,
                        transitionType: Int
                ) = Unit

                override fun endTransition(
                        transition: LayoutTransition?,
                        container: ViewGroup?,
                        view: View?,
                        transitionType: Int
                ) {
                    // Update UI before visibility change
                    setCurrentlySelectedCurrency(cryptoCurrency)
                    // Fade in title
                    val alphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }
                    textview_selected_currency.startAnimation(alphaAnimation)
                    alphaAnimation.setAnimationListener {
                        onAnimationEnd { textview_selected_currency.alpha = 1.0f }
                    }

                    // Remove listener
                    removeTransitionListener(this)
                }
            })
        }

        // Trigger layout change
        animateCoinSelectionLayout(View.GONE)
    }

    private fun animateCoinSelectionLayout(@ViewUtils.Visibility visibility: Int) {
        TransitionManager.beginDelayedTransition(
                constraint_layout,
                AutoTransition().apply { duration = 500 }
        )
        ConstraintSet().apply {
            clone(constraint_layout)
            setVisibility(R.id.linear_layout_coin_selection, visibility)
            applyTo(constraint_layout)
        }
    }

}