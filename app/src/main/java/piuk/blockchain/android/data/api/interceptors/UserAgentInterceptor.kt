package piuk.blockchain.android.data.api.interceptors

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response
import piuk.blockchain.android.BuildConfig

class UserAgentInterceptor : Interceptor {

    /**
     * Inserts a pre-formatted header into all web requests, matching the pattern
     * "Blockchain-Android/6.4.2 (Android 5.0.1)".
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val userAgent = "Blockchain-Android/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})"

        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
        return chain.proceed(requestWithUserAgent)
    }

}