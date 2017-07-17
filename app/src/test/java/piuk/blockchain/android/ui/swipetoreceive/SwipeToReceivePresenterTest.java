package piuk.blockchain.android.ui.swipetoreceive;

import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.ui.base.UiState;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SwipeToReceivePresenterTest {

    private SwipeToReceivePresenter subject;
    @Mock private SwipeToReceiveView activity;
    @Mock private SwipeToReceiveHelper swipeToReceiveHelper;
    @Mock private QrCodeDataManager qrCodeDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new SwipeToReceivePresenter(qrCodeDataManager, swipeToReceiveHelper);
        subject.initView(activity);
    }

    @Test
    public void onViewReadyNoAddresses() throws Exception {
        // Arrange
        when(swipeToReceiveHelper.getReceiveAddresses()).thenReturn(Collections.emptyList());
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).setUiState(UiState.LOADING);
        verify(activity).setUiState(UiState.EMPTY);
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
        verify(activity).setUiState(UiState.LOADING);
        verify(activity).displayReceiveAccount("Account");
        verify(activity).setUiState(UiState.FAILURE);
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
        verify(activity).setUiState(UiState.LOADING);
        verify(activity).displayReceiveAccount("Account");
        verify(activity).displayQrCode(any(Bitmap.class));
        verify(activity).setUiState(UiState.CONTENT);
        verify(activity).displayReceiveAddress("addr0");
    }

}