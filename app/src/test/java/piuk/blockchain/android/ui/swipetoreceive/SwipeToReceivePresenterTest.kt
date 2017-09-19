package piuk.blockchain.android.ui.swipetoreceive

import android.graphics.Bitmap
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager
import piuk.blockchain.android.ui.base.UiState

class SwipeToReceivePresenterTest {

    private lateinit var subject: SwipeToReceivePresenter
    private val activity: SwipeToReceiveView = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private val qrCodeDataManager: QrCodeDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        subject = SwipeToReceivePresenter(qrCodeDataManager, swipeToReceiveHelper)
        subject.initView(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady no addresses`() {
        // Arrange
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(emptyList())
        // Act
        subject.onViewReady()
        // Assert
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).setUiState(UiState.EMPTY)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady address returned is empty`() {
        // Arrange
        val addresses = listOf("adrr0", "addr1", "addr2", "addr3", "addr4")
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(addresses)
        whenever(swipeToReceiveHelper.getBitcoinAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle())
                .thenReturn(Single.just(""))
        whenever(activity.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onViewReady()
        // Assert
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).cryptoCurrency
        verify(activity).displayReceiveAccount("Account")
        verify(activity).setUiState(UiState.FAILURE)
    }

    @Test
    @Throws(Exception::class)
    fun `onView ready address returned BTC`() {
        // Arrange
        val bitmap: Bitmap = mock()
        val addresses = listOf("adrr0", "addr1", "addr2", "addr3", "addr4")
        whenever(swipeToReceiveHelper.getBitcoinReceiveAddresses()).thenReturn(addresses)
        whenever(swipeToReceiveHelper.getBitcoinAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getNextAvailableBitcoinAddressSingle())
                .thenReturn(Single.just("addr0"))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.just(bitmap))
        whenever(activity.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
        // Act
        subject.onViewReady()
        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).cryptoCurrency
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("addr0")
        verifyNoMoreInteractions(activity)
    }

    @Test
    @Throws(Exception::class)
    fun `onView ready address returned ETH`() {
        // Arrange
        val address = "addr0"
        val bitmap: Bitmap = mock()
        whenever(swipeToReceiveHelper.getEthReceiveAddress()).thenReturn(address)
        whenever(swipeToReceiveHelper.getEthAccountName()).thenReturn("Account")
        whenever(swipeToReceiveHelper.getEthReceiveAddressSingle())
                .thenReturn(Single.just(address))
        whenever(qrCodeDataManager.generateQrCode(anyString(), anyInt()))
                .thenReturn(Observable.just(bitmap))
        whenever(activity.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
        // Act
        subject.onViewReady()
        // Assert
        verify(qrCodeDataManager).generateQrCode(anyString(), anyInt())
        verifyNoMoreInteractions(qrCodeDataManager)
        verify(activity).setUiState(UiState.LOADING)
        verify(activity).cryptoCurrency
        verify(activity).displayReceiveAccount("Account")
        verify(activity).displayQrCode(bitmap)
        verify(activity).setUiState(UiState.CONTENT)
        verify(activity).displayReceiveAddress("addr0")
        verifyNoMoreInteractions(activity)
    }

}