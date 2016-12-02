package piuk.blockchain.android.injection;

import com.google.gson.Gson;

import info.blockchain.api.MetadataEndpoints;
import info.blockchain.api.Notifications;
import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import piuk.blockchain.android.data.access.AccessState;
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
    @Named("api")
    protected Retrofit provideRetrofitApiInstance() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        // TODO: 02/12/2016 For now this only provides the metadata dev URL, this will change
        return new Retrofit.Builder()
                .baseUrl(MetadataEndpoints.API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    @Named("server")
    protected Retrofit provideRetrofitBlockchainInstance() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseServerUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    protected Gson provideGsonInstance() {
        return new Gson();
    }

}
