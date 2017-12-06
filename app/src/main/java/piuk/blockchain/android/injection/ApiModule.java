package piuk.blockchain.android.injection;

import android.util.Log;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.shapeshift.ShapeShiftUrls;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.ConnectionApi;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.api.interceptors.ApiInterceptor;
import piuk.blockchain.android.data.api.interceptors.UserAgentInterceptor;
import piuk.blockchain.android.data.notifications.NotificationService;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.TLSSocketFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;


@SuppressWarnings("WeakerAccess")
@Module
public class ApiModule {

    private static final String TAG = ApiModule.class.getSimpleName();
    private static final int API_TIMEOUT = 30;
    private static final int PING_INTERVAL = 10;

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected NotificationTokenManager provideNotificationTokenManager(AccessState accessState,
                                                                       PayloadManager payloadManager,
                                                                       PrefsUtil prefsUtil,
                                                                       RxBus rxBus) {

        return new NotificationTokenManager(
                new NotificationService(new WalletApi()),
                accessState,
                payloadManager,
                prefsUtil,
                rxBus);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient() {
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add("api.blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
                .add("blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .certificatePinner(certificatePinner)
                // Add logging for debugging purposes
                .addInterceptor(new ApiInterceptor())
                // Add header in all requests
                .addInterceptor(new UserAgentInterceptor());

        /*
          Enable TLS specific version V.1.2
          Issue Details : https://github.com/square/okhttp/issues/1934
         */
        try {
            TLSSocketFactory tlsSocketFactory = new TLSSocketFactory();
            builder.sslSocketFactory(tlsSocketFactory, tlsSocketFactory.systemDefaultTrustManager());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to create Socket connection ", e);
        }

        return builder.build();
    }

    @Provides
    @Singleton
    protected JacksonConverterFactory provideJacksonConverterFactory() {
        return JacksonConverterFactory.create();
    }

    @Provides
    @Singleton
    protected RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
        return RxJava2CallAdapterFactory.create();
    }

    @Provides
    @Singleton
    @Named("api")
    protected Retrofit provideRetrofitApiInstance(OkHttpClient okHttpClient,
                                                  JacksonConverterFactory converterFactory,
                                                  RxJava2CallAdapterFactory rxJavaCallFactory,
                                                  EnvironmentSettings environmentSettings) {

        return new Retrofit.Builder()
                .baseUrl(environmentSettings.getApiUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("explorer")
    protected Retrofit provideRetrofitExplorerInstance(OkHttpClient okHttpClient,
                                                       JacksonConverterFactory converterFactory,
                                                       RxJava2CallAdapterFactory rxJavaCallFactory,
                                                       EnvironmentSettings environmentSettings) {
        return new Retrofit.Builder()
                .baseUrl(environmentSettings.getExplorerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    protected SSLVerifyUtil provideSSlVerifyUtil(@Named("explorer") Retrofit retrofit,
                                                 RxBus rxBus) {

        return new SSLVerifyUtil(rxBus, new ConnectionApi(retrofit));
    }

    @Provides
    @Singleton
    @Named("shapeshift")
    protected Retrofit provideRetrofitShapeShiftInstance(OkHttpClient okHttpClient,
                                                         JacksonConverterFactory converterFactory,
                                                         RxJava2CallAdapterFactory rxJavaCallFactory) {
        return new Retrofit.Builder()
                .baseUrl(ShapeShiftUrls.SHAPESHIFT_URL)
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }
}
