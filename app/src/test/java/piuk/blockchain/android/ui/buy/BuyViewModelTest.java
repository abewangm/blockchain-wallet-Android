package piuk.blockchain.android.ui.buy;

import android.app.Application;

import info.blockchain.wallet.payload.PayloadManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.util.AppUtil;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PrivateMemberAccessBetweenOuterAndInnerClass", "AnonymousInnerClassMayBeStatic", "unchecked"})
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class BuyViewModelTest {

    private BuyViewModel subject;

    @Mock private BuyViewModel.DataListener activity;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private BuyDataManager buyDataManager;
    @Mock private AppUtil appUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new BuyViewModelTest.MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new BuyViewModelTest.MockDataManagerModule());

        subject = new BuyViewModel(activity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange
        WebViewLoginDetails webViewLoginDetails = new WebViewLoginDetails("","",
                "","");
        when(payloadDataManager.loadNodes()).thenReturn(
                Observable.just(true));
        when(buyDataManager.getWebViewLoginDetails()).thenReturn(
                Observable.just(webViewLoginDetails));

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).setWebViewLoginDetails(webViewLoginDetails);
    }

    @Test
    public void onViewReady_secondPassword() throws Exception {
        // Arrange
        WebViewLoginDetails webViewLoginDetails = new WebViewLoginDetails("","",
                "","");
        when(payloadDataManager.loadNodes()).thenReturn(
                Observable.just(false));
        when(buyDataManager.getWebViewLoginDetails()).thenReturn(
                Observable.just(webViewLoginDetails));
        when(payloadDataManager.isDoubleEncrypted()).thenReturn(
                false);

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showSecondPasswordDialog();
        verify(activity).setUiState(UiState.EMPTY);
    }

    private class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected BuyDataManager provideBuyDataManager(SettingsDataManager settingsDataManager,
                                                       AuthDataManager authDataManager,
                                                       PayloadDataManager payloadDataManager,
                                                       AccessState accessState) {
            return buyDataManager;
        }

        @Override
        protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager,
                                                               RxBus rxBus) {
            return payloadDataManager;
        }
    }
}
