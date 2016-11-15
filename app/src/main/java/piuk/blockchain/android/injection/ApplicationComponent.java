package piuk.blockchain.android.injection;

import info.blockchain.wallet.util.PrivateKeyFactory;

import javax.inject.Singleton;

import dagger.Component;
import piuk.blockchain.android.data.api.UrlSettings;
import piuk.blockchain.android.data.notifications.InstanceIdService;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.exceptions.LoggingExceptionHandler;

/**
 * Created by adambennett on 08/08/2016.
 */

@SuppressWarnings("WeakerAccess")
@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class
})
public interface ApplicationComponent {

    DataManagerComponent plus(DataManagerModule userModule);

    void inject(AppUtil appUtil);

    void inject(LoggingExceptionHandler loggingExceptionHandler);

    void inject(ExchangeRateFactory exchangeRateFactory);

    void inject(ReceiveCurrencyHelper receiveCurrencyHelper);

    void inject(UrlSettings urlSettings);

    void inject(PrivateKeyFactory privateKeyFactory);

    void inject(InstanceIdService instanceIdService);
}
