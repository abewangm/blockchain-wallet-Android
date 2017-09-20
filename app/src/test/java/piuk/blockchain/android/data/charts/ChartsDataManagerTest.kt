package piuk.blockchain.android.data.charts

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.Scale
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.rxjava.RxBus

class ChartsDataManagerTest: RxTest() {

    private lateinit var subject: ChartsDataManager
    private val historicPriceApi: PriceApi = mock()
    private val rxBus = RxBus()

    @Before
    override fun setUp() {
        super.setUp()
        subject = ChartsDataManager(historicPriceApi, rxBus)
    }

    @Test
    @Throws(Exception::class)
    fun getYearPrice() {
        // Arrange
        val btc = CryptoCurrencies.BTC
        val fiat = "USD"
        whenever(historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_DAY)
        )).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getYearPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_DAY)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    @Throws(Exception::class)
    fun getMonthPrice() {
        // Arrange
        val btc = CryptoCurrencies.BTC
        val fiat = "USD"
        whenever(historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.TWO_HOURS)
        )).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getMonthPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.TWO_HOURS)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    @Throws(Exception::class)
    fun getWeekPrice() {
        // Arrange
        val btc = CryptoCurrencies.BTC
        val fiat = "USD"
        whenever(historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_HOUR)
        )).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getWeekPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.ONE_HOUR)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

    @Test
    @Throws(Exception::class)
    fun getDayPrice() {
        // Arrange
        val btc = CryptoCurrencies.BTC
        val fiat = "USD"
        whenever(historicPriceApi.getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.FIFTEEN_MINUTES)
        )).thenReturn(Observable.just(listOf(PriceDatum())))
        // Act
        val testObserver = subject.getDayPrice(btc, fiat).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(historicPriceApi).getHistoricPriceSeries(
                eq(btc.symbol),
                eq(fiat),
                any(),
                eq(Scale.FIFTEEN_MINUTES)
        )
        verifyNoMoreInteractions(historicPriceApi)
    }

}