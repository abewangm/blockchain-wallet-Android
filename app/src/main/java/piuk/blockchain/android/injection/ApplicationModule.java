package piuk.blockchain.android.injection;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

import info.blockchain.wallet.util.PrivateKeyFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.currency.CurrencyState;
import piuk.blockchain.android.data.ethereum.EthereumAccountWrapper;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MetadataUtils;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;


@SuppressWarnings("WeakerAccess")
@Module
public class ApplicationModule {

    private final Application application;

    public ApplicationModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    protected Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    protected PrefsUtil providePrefsUtil() {
        return new PrefsUtil(application);
    }

    @Provides
    @Singleton
    protected AppUtil provideAppUtil() {
        return new AppUtil(application);
    }

    @Provides
    protected AccessState provideAccessState() {
        return AccessState.getInstance();
    }

    @Provides
    protected AESUtilWrapper provideAesUtils() {
        return new AESUtilWrapper();
    }

    @Provides
    protected StringUtils provideStringUtils() {
        return new StringUtils(application);
    }

    @Provides
    @Singleton
    protected DynamicFeeCache provideDynamicFeeCache() {
        return new DynamicFeeCache();
    }

    @Provides
    protected ExchangeRateFactory provideExchangeRateFactory() {
        return ExchangeRateFactory.getInstance();
    }

    @Provides
    protected PrivateKeyFactory privateKeyFactory() {
        return new PrivateKeyFactory();
    }

    @Provides
    @Singleton
    protected NotificationManager provideNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @Singleton
    protected RxBus provideRxBus() {
        return new RxBus();
    }

    @Provides
    @Singleton
    protected EnvironmentSettings provideDebugSettings() {
        return new EnvironmentSettings();
    }

    @Provides
    protected CurrencyState provideCurrencyState() {
        return CurrencyState.getInstance();
    }

    @Provides
    protected MetadataUtils provideMetadataUtils() {
        return new MetadataUtils();
    }

    @Provides
    protected EthereumAccountWrapper provideEthereumAccountWrapper() {
        return new EthereumAccountWrapper();
    }
}
