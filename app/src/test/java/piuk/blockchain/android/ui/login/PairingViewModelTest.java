//package piuk.blockchain.android.ui.pairing;
//
//import android.app.Application;
//
//import info.blockchain.wallet.payload.PayloadManager;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Answers;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.robolectric.RobolectricTestRunner;
//import org.robolectric.RuntimeEnvironment;
//import org.robolectric.annotation.Config;
//
//import javax.net.ssl.SSLPeerUnverifiedException;
//
//import io.reactivex.Completable;
//import piuk.blockchain.android.BlockchainTestApplication;
//import piuk.blockchain.android.BuildConfig;
//import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
//import piuk.blockchain.android.data.rxjava.RxBus;
//import piuk.blockchain.android.injection.ApiModule;
//import piuk.blockchain.android.injection.ApplicationModule;
//import piuk.blockchain.android.injection.DataManagerModule;
//import piuk.blockchain.android.injection.Injector;
//import piuk.blockchain.android.injection.InjectorTestUtils;
//import piuk.blockchain.android.ui.customviews.ToastCustom;
//import piuk.blockchain.android.util.AppUtil;
//import piuk.blockchain.android.util.PrefsUtil;
//
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.atLeastOnce;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoMoreInteractions;
//import static org.mockito.Mockito.verifyZeroInteractions;
//import static org.mockito.Mockito.when;
//
//@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
//@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
//@RunWith(RobolectricTestRunner.class)
//public class PairingViewModelTest {
//
//    private PairingViewModel subject;
//    @Mock private AppUtil appUtil;
//    @Mock private PrefsUtil prefsUtil;
//    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PayloadDataManager payloadDataManager;
//    @Mock private PairingViewModel.DataListener activity;
//
//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//
//        InjectorTestUtils.initApplicationComponent(
//                Injector.getInstance(),
//                new MockApplicationModule(RuntimeEnvironment.application),
//                new ApiModule(),
//                new MockDataManagerModule());
//
//        subject = new PairingViewModel(activity);
//    }
//
//    @Test
//    public void pairWithQRSuccess() throws Exception {
//        // Arrange
//        String qrCode = "QR_CODE";
//        String sharedKey = "SHARED_KEY";
//        String guid = "GUID";
//        when(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.complete());
//        when(payloadDataManager.getWallet().getSharedKey()).thenReturn(sharedKey);
//        when(payloadDataManager.getWallet().getGuid()).thenReturn(guid);
//        // Act
//        subject.pairWithQR(qrCode);
//        // Assert
//        verify(activity).showProgressDialog(anyInt());
//        verify(activity).dismissProgressDialog();
//        verify(activity).startPinEntryActivity();
//        verifyNoMoreInteractions(activity);
//        verify(appUtil).clearCredentials();
//        verify(appUtil).setSharedKey(sharedKey);
//        verifyNoMoreInteractions(appUtil);
//        verify(prefsUtil).setValue(PrefsUtil.KEY_GUID, guid);
//        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
//        verify(prefsUtil).setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true);
//        verifyNoMoreInteractions(prefsUtil);
//        verify(payloadDataManager).handleQrCode(qrCode);
//        verify(payloadDataManager, atLeastOnce()).getWallet();
//        verifyNoMoreInteractions(payloadDataManager);
//    }
//
//    @Test
//    public void pairWithQRFailure() throws Exception {
//        // Arrange
//        String qrCode = "QR_CODE";
//        when(payloadDataManager.handleQrCode(qrCode)).thenReturn(Completable.error(new Throwable()));
//        // Act
//        subject.pairWithQR(qrCode);
//        // Assert
//        verify(activity).showProgressDialog(anyInt());
//        verify(activity).dismissProgressDialog();
//        //noinspection WrongConstant
//        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
//        verifyNoMoreInteractions(activity);
//        verify(appUtil).clearCredentials();
//        verify(appUtil).clearCredentialsAndRestart();
//        verifyNoMoreInteractions(appUtil);
//        verifyZeroInteractions(prefsUtil);
//        verify(payloadDataManager).handleQrCode(qrCode);
//        verifyNoMoreInteractions(payloadDataManager);
//    }
//
//    @Test
//    public void pairWithQRSslException() throws Exception {
//        // Arrange
//        String qrCode = "QR_CODE";
//        when(payloadDataManager.handleQrCode(qrCode))
//                .thenReturn(Completable.error(new SSLPeerUnverifiedException("")));
//        // Act
//        subject.pairWithQR(qrCode);
//        // Assert
//        verify(activity).showProgressDialog(anyInt());
//        verify(activity).dismissProgressDialog();
//        verifyNoMoreInteractions(activity);
//        verify(appUtil, times(2)).clearCredentials();
//        verifyNoMoreInteractions(appUtil);
//        verifyZeroInteractions(prefsUtil);
//        verify(payloadDataManager).handleQrCode(qrCode);
//        verifyNoMoreInteractions(payloadDataManager);
//        verify(activity).showProgressDialog(anyInt());
//        verify(activity).dismissProgressDialog();
//        verifyNoMoreInteractions(appUtil);
//    }
//
//    @SuppressWarnings("SyntheticAccessorCall")
//    private class MockApplicationModule extends ApplicationModule {
//        public MockApplicationModule(Application application) {
//            super(application);
//        }
//
//        @Override
//        protected AppUtil provideAppUtil() {
//            return appUtil;
//        }
//
//        @Override
//        protected PrefsUtil providePrefsUtil() {
//            return prefsUtil;
//        }
//    }
//
//    @SuppressWarnings("SyntheticAccessorCall")
//    private class MockDataManagerModule extends DataManagerModule {
//        @Override
//        protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager,
//                                                               RxBus rxBus) {
//            return payloadDataManager;
//        }
//    }
//}