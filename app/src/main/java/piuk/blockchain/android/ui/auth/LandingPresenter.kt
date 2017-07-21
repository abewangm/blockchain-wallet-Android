package piuk.blockchain.android.ui.auth

import android.content.Context
import io.reactivex.Observable
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.datamanagers.PromptManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import javax.inject.Inject

class LandingPresenter @Inject constructor(
        private val appUtil: AppUtil,
        private val environmentSettings: EnvironmentSettings,
        private val promptManager: PromptManager
) : BasePresenter<LandingView>() {

    override fun onViewReady() {

        if (environmentSettings.shouldShowDebugMenu()) {
            view.showToast("Current environment: " + environmentSettings.environment.getName(), ToastCustom.TYPE_GENERAL)
            view.showDebugMenu()
        }
    }

    fun initPreLoginPrompts(context: Context) {
        promptManager.getPreLoginPrompts(context)
                .flatMap { Observable.fromIterable(it) }
                .forEach { view.showWarningPrompt(it) }

    }

    fun getAppUtil(): AppUtil {
        return appUtil
    }
}
