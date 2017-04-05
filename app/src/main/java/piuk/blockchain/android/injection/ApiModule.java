package piuk.blockchain.android.injection;

import android.util.Log;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.payload.PayloadManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.ApiInterceptor;
import piuk.blockchain.android.data.api.ConnectionApi;
import piuk.blockchain.android.data.api.DebugSettings;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.services.ContactsService;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.data.stores.PendingTransactionListStore;
import piuk.blockchain.android.data.stores.TransactionListStore;
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

    /**
     * This should be phased out for {@link PayloadDataManager}
     */
    @Provides
    @Deprecated
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected TransactionListStore provideTransactionListStore() {
        return new TransactionListStore();
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

    // TODO: 09/02/2017 This should be moved to DataManagerModule eventually
    @Provides
    @Singleton
    protected ContactsDataManager provideContactsManager(RxBus rxBus) {
        return new ContactsDataManager(
                new ContactsService(new Contacts()),
                new PendingTransactionListStore(),
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
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .certificatePinner(certificatePinner)
                .addInterceptor(new ApiInterceptor());

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
                                                  DebugSettings debugSettings) {

        return new Retrofit.Builder()
                .baseUrl(debugSettings.getBaseApiUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("server")
    protected Retrofit provideRetrofitBlockchainInstance(OkHttpClient okHttpClient,
                                                         JacksonConverterFactory converterFactory,
                                                         RxJava2CallAdapterFactory rxJavaCallFactory,
                                                         DebugSettings debugSettings) {
        return new Retrofit.Builder()
                .baseUrl(debugSettings.getBaseServerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    protected SSLVerifyUtil provideSSlVerifyUtil(@Named("server") Retrofit retrofit,
                                                 RxBus rxBus) {

        return new SSLVerifyUtil(rxBus, new ConnectionApi(retrofit));
    }

}
