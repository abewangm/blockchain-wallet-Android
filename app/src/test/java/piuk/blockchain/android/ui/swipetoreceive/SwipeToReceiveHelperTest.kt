package piuk.blockchain.android.ui.swipetoreceive

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.Balance
import info.blockchain.wallet.payload.data.Account
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import piuk.blockchain.android.R
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ACCOUNT_NAME
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ADDRESSES
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.Companion.KEY_SWIPE_RECEIVE_ETH_ADDRESS
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import java.math.BigInteger
import java.util.*

class SwipeToReceiveHelperTest : RxTest() {

    private lateinit var subject: SwipeToReceiveHelper
    private val payloadDataManager: PayloadDataManager = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val stringUtils: StringUtils = mock()
    private val ethDataManager: EthDataManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val bchDataManager: BchDataManager = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = SwipeToReceiveHelper(
                payloadDataManager,
                prefsUtil,
                ethDataManager,
                bchDataManager,
                stringUtils
        )
    }

    @Test
    @Throws(Exception::class)
    fun updateAndStoreBitcoinAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
                .thenReturn(true)
        val mockAccount: Account = mock()
        whenever(payloadDataManager.defaultAccount).thenReturn(mockAccount)
        whenever(mockAccount.label).thenReturn("Account")
        whenever(payloadDataManager.getReceiveAddressAtPosition(eq(mockAccount), anyInt()))
                .thenReturn("address")
        // Act
        subject.updateAndStoreBitcoinAddresses()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)
        verify(payloadDataManager, times(5)).getReceiveAddressAtPosition(eq(mockAccount), anyInt())
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "Account")
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ADDRESSES, "address,address,address,address,address,")
    }

    @Test
    @Throws(Exception::class)
    fun storeEthAddress() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true))
                .thenReturn(true)
        whenever(ethDataManager.getEthWallet()?.account?.address).thenReturn("address")
        // Act
        subject.storeEthAddress()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)
        verify(ethDataManager, atLeastOnce()).getEthWallet()
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, "address")
    }

    @Test
    @Throws(Exception::class)
    fun getNextAvailableAddressSingleValid() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(0L) }
        map.put("addr0", balance0)
        map.put("addr1", balance1)
        map.put("addr2", balance2)
        map.put("addr3", balance3)
        map.put("addr4", balance4)
        whenever(payloadDataManager.getBalanceOfAddresses(anyList()))
                .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
                .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val testObserver = subject.getNextAvailableBitcoinAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("addr3")
    }

    @Test
    @Throws(Exception::class)
    fun getNextAvailableAddressSingleAllUsed() {
        // Arrange
        val map = LinkedHashMap<String, Balance>()
        val balance0 = Balance().apply { finalBalance = BigInteger.valueOf(1000L) }
        val balance1 = Balance().apply { finalBalance = BigInteger.valueOf(5L) }
        val balance2 = Balance().apply { finalBalance = BigInteger.valueOf(-10L) }
        val balance3 = Balance().apply { finalBalance = BigInteger.valueOf(1L) }
        val balance4 = Balance().apply { finalBalance = BigInteger.valueOf(1_000_000_000_000L) }
        map.put("addr0", balance0)
        map.put("addr1", balance1)
        map.put("addr2", balance2)
        map.put("addr3", balance3)
        map.put("addr4", balance4)
        whenever(payloadDataManager.getBalanceOfAddresses(anyList()))
                .thenReturn(Observable.just(map))
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
                .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val testObserver = subject.getNextAvailableBitcoinAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue("")
    }

    @Test
    @Throws(Exception::class)
    fun getEthReceiveAddressSingle() {
        // Arrange
        val address = "ADDRESS"
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null))
                .thenReturn(address)
        // Act
        val testObserver = subject.getEthReceiveAddressSingle().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(address)
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinReceiveAddresses() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
                .thenReturn("addr0, addr1, addr2, addr3, addr4")
        // Act
        val result = subject.getBitcoinReceiveAddresses()
        // Assert
        assertEquals(5, result.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinReceiveAddressesEmptyList() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, ""))
                .thenReturn("")
        // Act
        val result = subject.getBitcoinReceiveAddresses()
        // Assert
        assertEquals(emptyList<Any>(), result)
    }

    @Test
    @Throws(Exception::class)
    fun getEthReceiveAddress() {
        // Arrange
        val address = "ADDRESS"
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null))
                .thenReturn(address)
        // Act
        val result = subject.getEthReceiveAddress()
        // Assert
        verify(prefsUtil).getValue(KEY_SWIPE_RECEIVE_ETH_ADDRESS, null)
        result `should equal` address
    }

    @Test
    @Throws(Exception::class)
    fun getBitcoinAccountName() {
        // Arrange
        whenever(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, ""))
                .thenReturn("Account")
        // Act
        val result = subject.getBitcoinAccountName()
        // Assert
        result `should equal` "Account"
    }

    @Test
    @Throws(Exception::class)
    fun getEthAccountName() {
        // Arrange
        val label = "LABEL"
        whenever(stringUtils.getString(R.string.eth_default_account_label)).thenReturn(label)
        // Act
        val result = subject.getEthAccountName()
        // Assert
        result `should equal` label
    }

}