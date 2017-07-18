package piuk.blockchain.android.ui.confirm

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.UiState

class ConfirmPaymentPresenterTest {

    private lateinit var subject: ConfirmPaymentPresenter
    private val mockActivity: ConfirmPaymentView = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = ConfirmPaymentPresenter()
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady payment details null`() {
        // Arrange

        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).closeDialog()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReady() {
        // Arrange
        val fromLabel = "FROM_LABEL"
        val toLabel = "TO_LABEL"
        val btcAmount = "BTC_AMOUNT"
        val btcUnit = "BTC_UNIT"
        val fiatAmount = "FIAT_AMOUNT"
        val fiatSymbol = "FIAT_SYMBOL"
        val btcFee = "BTC_FEE"
        val fiatFee = "FIAT_FEE"
        val btcTotal = "BTC_TOTAL"
        val fiatTotal = "FIAT_TOTAL"
        val confirmationDetails = PaymentConfirmationDetails().apply {
            this.fromLabel = fromLabel
            this.toLabel = toLabel
            this.btcAmount = btcAmount
            this.btcUnit = btcUnit
            this.fiatAmount = fiatAmount
            this.fiatSymbol = fiatSymbol
            this.btcFee = btcFee
            this.fiatFee = fiatFee
            this.btcTotal = btcTotal
            this.fiatTotal = fiatTotal
        }
        whenever(mockActivity.paymentDetails).thenReturn(confirmationDetails)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockActivity).paymentDetails
        verify(mockActivity).setFromLabel(fromLabel)
        verify(mockActivity).setToLabel(toLabel)
        verify(mockActivity).setAmount("$btcAmount $btcUnit ($fiatSymbol$fiatAmount)")
        verify(mockActivity).setFee("$btcFee $btcUnit ($fiatSymbol$fiatFee)")
        verify(mockActivity).setTotalBtc("$btcTotal $btcUnit")
        verify(mockActivity).setTotalFiat("$fiatSymbol$fiatTotal")
        verify(mockActivity).setUiState(UiState.CONTENT)
        verifyNoMoreInteractions(mockActivity)
    }

}