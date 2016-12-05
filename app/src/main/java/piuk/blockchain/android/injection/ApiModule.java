package piuk.blockchain.android.injection;

import com.google.gson.Gson;

import info.blockchain.api.MetadataEndpoints;
import info.blockchain.api.Notifications;
import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.ApiInterceptor;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.util.PrefsUtil;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApiModule {

    private static final int API_TIMEOUT = 15;

    @Provides
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
                                                                       PrefsUtil prefsUtil) {

        return new NotificationTokenManager(
                new NotificationService(new Notifications()), accessState, payloadManager, prefsUtil);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new ApiInterceptor())
                .build();
    }

    @Provides
    @Singleton
    protected Gson provideGsonInstance() {
        return new Gson();
    }

    @Provides
    @Singleton
    protected GsonConverterFactory provideGsonConverterFactory(Gson gson) {
        return GsonConverterFactory.create(gson);
    }

    @Provides
    @Singleton
    @Named("api")
    protected Retrofit provideRetrofitApiInstance(OkHttpClient okHttpClient, GsonConverterFactory converterFactory) {
        // TODO: 02/12/2016 For now this only provides the metadata dev URL, this will change
        return new Retrofit.Builder()
                .baseUrl(MetadataEndpoints.API_URL)
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("server")
    protected Retrofit provideRetrofitBlockchainInstance(OkHttpClient okHttpClient, GsonConverterFactory converterFactory) {
        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseServerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

}
