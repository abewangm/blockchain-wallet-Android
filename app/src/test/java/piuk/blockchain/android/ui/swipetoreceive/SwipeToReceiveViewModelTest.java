package piuk.blockchain.android.ui.swipetoreceive;

import android.app.Application;
import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.PrefsUtil;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class SwipeToReceiveViewModelTest {

    private SwipeToReceiveViewModel subject;
    @Mock private SwipeToReceiveViewModel.DataListener activity;
    @Mock private SwipeToReceiveHelper swipeToReceiveHelper;
    @Mock private QrCodeDataManager qrCodeDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        subject = new SwipeToReceiveViewModel(activity);
    }

    @Test
    public void onViewReadyNoAddresses() throws Exception {
        // Arrange
        when(swipeToReceiveHelper.getReceiveAddresses()).thenReturn(Collections.emptyList());
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).displayLoading();
        verify(activity).showNoAddressesAvailable();
    }

    @Test
    public void onViewReadyAddressReturnedIsEmpty() throws Exception {
        // Arrange
        List<String> addresses = Arrays.asList("adrr0", "addr1", "addr2", "addr3", "addr4");
        when(swipeToReceiveHelper.getReceiveAddresses()).thenReturn(addresses);
        when(swipeToReceiveHelper.getAccountName()).thenReturn("Account");
        when(swipeToReceiveHelper.getNextAvailableAddress()).thenReturn(Observable.just(""));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).displayLoading();
        verify(activity).displayReceiveAccount("Account");
        verify(activity).showNoAddressesAvailable();
    }

    @Test
    public void onViewReadyAddressReturned() throws Exception {
        // Arrange
        List<String> addresses = Arrays.asList("adrr0", "addr1", "addr2", "addr3", "addr4");
        when(swipeToReceiveHelper.getReceiveAddresses()).thenReturn(addresses);
        when(swipeToReceiveHelper.getAccountName()).thenReturn("Account");
        when(swipeToReceiveHelper.getNextAvailableAddress()).thenReturn(Observable.just("addr0"));
        when(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(mock(Bitmap.class)));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).displayLoading();
        verify(activity).displayReceiveAccount("Account");
        verify(activity).displayQrCode(any(Bitmap.class));
        verify(activity).displayReceiveAddress("addr0");
    }

    private static class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected SwipeToReceiveHelper provideSwipeToReceiveHelper(PayloadDataManager payloadDataManager,
                                                                   PrefsUtil prefsUtil) {
            return swipeToReceiveHelper;
        }

        @Override
        protected QrCodeDataManager provideQrDataManager() {
            return qrCodeDataManager;
        }
    }
}