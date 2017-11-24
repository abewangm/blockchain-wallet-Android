package piuk.blockchain.android.ui.shapeshift.models

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes

data class TradeProgressUiState(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @DrawableRes val icon: Int,
        val showSteps: Boolean,
        val stepNumber: Int
)