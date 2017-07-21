package piuk.blockchain.android.data.answers

import com.crashlytics.android.answers.*
import piuk.blockchain.android.BuildConfig

/**
 * A singleton wrapper for the [Answers] client. All events will only be logged for release or
 * dogfood builds.
 *
 * Note: absolutely no identifying information should be included in an [AnswersEvent], ever.
 * These should be used to get a feel for how often features are used, but that's it.
 */
object Logging {

    private val shouldLog = BuildConfig.USE_CRASHLYTICS || BuildConfig.DOGFOOD

    fun logCustom(customEvent: CustomEvent) {
        if (shouldLog) Answers.getInstance().logCustom(customEvent)
    }

    fun logContentView(contentViewEvent: ContentViewEvent) {
        if (shouldLog) Answers.getInstance().logContentView(contentViewEvent)
    }

    fun logLogin(loginEvent: LoginEvent) {
        if (shouldLog) Answers.getInstance().logLogin(loginEvent)
    }

    fun logSignUp(signUpEvent: SignUpEvent) {
        if (shouldLog) Answers.getInstance().logSignUp(signUpEvent)
    }

    fun logShare(shareEvent: ShareEvent) {
        if (shouldLog) Answers.getInstance().logShare(shareEvent)
    }

    fun logPurchase(purchaseEvent: PurchaseEvent) {
        if (shouldLog) Answers.getInstance().logPurchase(purchaseEvent)
    }

}