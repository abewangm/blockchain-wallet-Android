package piuk.blockchain.android.ui.receive;

import android.content.Intent;
import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.customviews.ToastCustom;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ReceiveQrPresenterTest {

    private ReceiveQrPresenter subject;
    @Mock private ReceiveQrView activity;
    @Mock private QrCodeDataManager qrCodeDataManager;
    @Mock private PayloadDataManager payloadDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new ReceiveQrPresenter(payloadDataManager, qrCodeDataManager);
        subject.initView(activity);
    }

    @Test
    public void onViewReadyWithIntent() throws Exception {
        // Arrange
        Intent intent = new Intent();
        String address = "address";
        String label = "label";
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, address);
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, label);
        when(activity.getPageIntent()).thenReturn(intent);
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
        when(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(bitmap));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setAddressInfo(address);
        verify(activity).setAddressLabel(label);
        verify(activity).setImageBitmap(bitmap);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyWithIntentBitmapException() throws Exception {
        // Arrange
        Intent intent = new Intent();
        String address = "address";
        String label = "label";
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, address);
        intent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, label);
        when(activity.getPageIntent()).thenReturn(intent);
        when(qrCodeDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setAddressInfo(address);
        verify(activity).setAddressLabel(label);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).finishActivity();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNoIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).finishActivity();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onCopyClicked() throws Exception {
        // Arrange
        String address = "address";
        subject.receiveAddressString = address;
        // Act
        subject.onCopyClicked();
        // Assert
        verify(activity).showClipboardWarning(address);
    }

    @Test
    public void getPayloadManager() throws Exception {
        // Arrange

        // Act
        PayloadDataManager result = subject.getPayloadDataManager();
        // Assert
        assertEquals(payloadDataManager, result);
    }

}