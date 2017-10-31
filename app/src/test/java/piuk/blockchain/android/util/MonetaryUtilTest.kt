package piuk.blockchain.android.util

import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigInteger
import java.text.NumberFormat
import java.util.*

class MonetaryUtilTest {

    val subject = MonetaryUtil(0)

    @Test
    @Throws(Exception::class)
    fun `updateUnit bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 2
    }

    @Test
    @Throws(Exception::class)
    fun `updateUnit mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 5
    }

    @Test
    @Throws(Exception::class)
    fun `updateUnit BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getBtcFormat()
        // Assert
        result.maximumFractionDigits `should equal to` 8
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun `updateUnit invalid format`() {
        // Arrange
        subject.updateUnit(7)
        // Act
        subject.getBtcFormat()
        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun getBTCFormat() {
        // Arrange

        // Act
        val result = subject.getBtcFormat()
        // Assert
        result `should be instance of` NumberFormat::class.java
    }

    @Test
    @Throws(Exception::class)
    fun getFiatFormat() {
        // Arrange
        val currency = "GBP"
        // Act
        val result = subject.getFiatFormat(currency)
        // Assert
        result.currency.currencyCode `should equal to` currency
    }

    @Test
    @Throws(Exception::class)
    fun getBTCUnits() {
        // Arrange

        // Act
        val result = subject.getBtcUnits()
        // Assert
        result.size `should equal to` 3
        result `should equal` arrayOf("BTC", "mBTC", "bits")
    }

    @Test
    @Throws(Exception::class)
    fun getBtcUnit() {
        // Arrange

        // Act
        val result = subject.getBtcUnit(MonetaryUtil.MICRO_BTC)
        // Assert
        result `should equal to` "bits"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmount bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "100.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmount mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "0.1"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmount BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmount(10_000L)
        // Assert
        result `should equal to` "0.0001"
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount long bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1)
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount long mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1000)
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount long BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000L)
        // Assert
        result `should equal` BigInteger.valueOf(1_000_000)
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount double bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1.0
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount double mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1000.0
    }

    @Test
    @Throws(Exception::class)
    fun `getUndenominatedAmount double BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getUndenominatedAmount(1_000_000.0)
        // Assert
        result `should equal` 1_000_000.0
    }

    @Test
    @Throws(Exception::class)
    fun `getDenominatedAmount bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1_000_000.0
    }

    @Test
    @Throws(Exception::class)
    fun `getDenominatedAmount mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1_000.0
    }

    @Test
    @Throws(Exception::class)
    fun `getDenominatedAmount BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDenominatedAmount(1.0)
        // Assert
        result `should equal` 1.0
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting long bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100,000,000.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting long mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100,000.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting long BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000L)
        // Assert
        result `should equal to` "100.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting double bits`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MICRO_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100,000,000.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting double mBTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.MILLI_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100,000.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getDisplayAmountWithFormatting double BTC`() {
        // Arrange
        subject.updateUnit(MonetaryUtil.UNIT_BTC)
        // Act
        val result = subject.getDisplayAmountWithFormatting(10_000_000_000.0)
        // Assert
        result `should equal` "100.0"
    }

    @Test
    @Throws(Exception::class)
    fun `getFiatDisplayString GBP in UK`() {
        // Arrange

        // Act
        val result = subject.getFiatDisplayString(1.2345, "GBP", Locale.UK)
        // Assert
        result `should equal` "£1.23"
    }

    @Test
    @Throws(Exception::class)
    fun `getCurrencySymbol GBP in UK`() {
        // Arrange

        // Act
        val result = subject.getCurrencySymbol("GBP", Locale.UK)
        // Assert
        result `should equal` "£"
    }

}