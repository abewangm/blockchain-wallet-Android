package piuk.blockchain.android.ui.chooser

import android.app.Application
import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.payload.PayloadManager
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.contacts.PaymentRequestType
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.injection.*
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import java.util.*


@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class AccountChooserViewModelTest {

    private lateinit var subject: AccountChooserViewModel
    private var mockActivity: AccountChooserViewModel.DataListener = mock()
    private var mockPrefsUtil: PrefsUtil = mock()
    private var mockWalletAccountHelper: WalletAccountHelper = mock()
    private var mockStringUtils: StringUtils = mock()
    private var mockContactsManager: ContactsDataManager = mock()
    private var mockAccessState: AccessState = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                MockApplicationModule(RuntimeEnvironment.application),
                MockApiModule(),
                MockDataManagerModule())

        subject = AccountChooserViewModel(mockActivity)
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun onViewReadyRequestTypeNull() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(null)
        // Act
        subject.onViewReady()
        // Assert

    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeContact() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
        val contact0 = Contact()
        contact0.mdid = "mdid"
        val contact1 = Contact()
        contact1.mdid = "mdid"
        val contact2 = Contact()
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value is 3 as only 2 confirmed contacts plus header
        captor.firstValue.size shouldEqual 3
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeContactNoConfirmedContacts() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        verify(mockActivity).showNoContacts()
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeRequest() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.REQUEST)
        val itemAccount0 = ItemAccount("", "", null, null, null, null)
        val itemAccount1 = ItemAccount("", "", null, null, null, null)
        val itemAccount2 = ItemAccount("", "", null, null, null, null)
        whenever(mockWalletAccountHelper.getHdAccounts(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size shouldEqual 8
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeSendContactsEnabled() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.SEND)
        whenever(mockActivity.ifContactsEnabled).thenReturn(true)
        val contact0 = Contact()
        contact0.mdid = "mdid"
        val contact1 = Contact()
        contact1.mdid = "mdid"
        val contact2 = Contact()
        whenever(mockContactsManager.contactList)
                .thenReturn(Observable.just(contact0, contact1, contact2))
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(mockWalletAccountHelper.getHdAccounts(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsManager).contactList
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 3 headers, 3 accounts, 3 legacy addresses, 2 confirmed contacts
        captor.firstValue.size shouldEqual 11
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeSendContactsDisabled() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.SEND)
        whenever(mockActivity.ifContactsEnabled).thenReturn(false)
        val itemAccount0 = ItemAccount()
        val itemAccount1 = ItemAccount()
        val itemAccount2 = ItemAccount()
        whenever(mockWalletAccountHelper.getHdAccounts(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any()))
                .thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verifyZeroInteractions(mockContactsManager)
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses, 0 Contacts
        captor.firstValue.size shouldEqual 8
    }

    inner class MockApplicationModule(application: Application?) : ApplicationModule(application) {
        override fun providePrefsUtil() = mockPrefsUtil

        override fun provideStringUtils() = mockStringUtils

        override fun provideAccessState() = mockAccessState
    }

    inner class MockDataManagerModule : DataManagerModule() {
        override fun provideWalletAccountHelper(
                payloadManager: PayloadManager?,
                                                prefsUtil: PrefsUtil?,
                                                stringUtils: StringUtils?,
                                                exchangeRateFactory: ExchangeRateFactory?
        ) = mockWalletAccountHelper
    }

    inner class MockApiModule : ApiModule() {
        override fun provideContactsManager(rxBus: RxBus?) = mockContactsManager
    }

}






