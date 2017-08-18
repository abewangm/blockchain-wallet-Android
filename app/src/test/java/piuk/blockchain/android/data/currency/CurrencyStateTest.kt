package piuk.blockchain.android.data.currency

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.util.PrefsUtil

class CurrencyStateTest : RxTest() {

    private lateinit var subject: CurrencyState
    private val mockPrefs: PrefsUtil = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = CurrencyState.getInstance()
    }

    @Test
    @Throws(Exception::class)
    fun getSelectedCryptoCurrencyDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.BTC.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.BTC)
    }

    @Test
    @Throws(Exception::class)
    fun getSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.ETHER)
    }

    @Test
    @Throws(Exception::class)
    fun getSetSelectedCryptoCurrencyBtc() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.setCryptoCurrency(CryptoCurrencies.BTC)
        // Assert
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.BTC)
    }

    @Test
    @Throws(Exception::class)
    fun getSetSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.setCryptoCurrency(CryptoCurrencies.ETHER)
        // Assert
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.ETHER)
    }

    @Test
    @Throws(Exception::class)
    fun isDisplayingCryptoDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertTrue(subject.isDisplayingCryptoCurrency())
    }

    @Test
    @Throws(Exception::class)
    fun isDisplayingCryptoFalse() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.setDisplayingCryptoCurrency(false)
        // Assert
        Assert.assertFalse(subject.isDisplayingCryptoCurrency())
    }

    @Test
    @Throws(Exception::class)
    fun isDisplayingCryptoTrue() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.setDisplayingCryptoCurrency(true)
        // Assert
        Assert.assertTrue(subject.isDisplayingCryptoCurrency())
    }


    @Test
    @Throws(Exception::class)
    fun toggleCryptoCurrency() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name)).thenReturn(CryptoCurrencies.BTC.name)
        subject.init(mockPrefs)
        // Act
        // Assert
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.BTC)
        subject.toggleCryptoCurrency()
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.ETHER)
        subject.toggleCryptoCurrency()
        Assert.assertEquals(subject.getCryptoCurrency(), CryptoCurrencies.BTC)
    }
}