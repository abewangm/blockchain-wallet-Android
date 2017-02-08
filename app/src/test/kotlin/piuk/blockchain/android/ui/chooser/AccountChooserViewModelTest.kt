package piuk.blockchain.android.ui.chooser

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.contacts.PaymentRequestType
import piuk.blockchain.android.data.datamanagers.ContactsDataManager
import piuk.blockchain.android.equals
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
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
    private var mockContactsDataManager: ContactsDataManager = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = AccountChooserViewModel(mockActivity).apply {
            prefsUtil = mockPrefsUtil
            walletAccountHelper = mockWalletAccountHelper
            stringUtils = mockStringUtils
            contactsDataManager = mockContactsDataManager
        }
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
        whenever(mockContactsDataManager.contactList).thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsDataManager).contactList
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value is 3 as only 2 confirmed contacts plus header
        captor.firstValue.size equals 3
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeContactNoConfirmedContacts() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
        val contact0 = Contact()
        val contact1 = Contact()
        val contact2 = Contact()
        whenever(mockContactsDataManager.contactList).thenReturn(Observable.just(contact0, contact1, contact2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockContactsDataManager).contactList
        verify(mockActivity).showNoContacts()
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeRequest() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.REQUEST)
        val itemAccount0 = ItemAccount("", "", null, null, null)
        val itemAccount1 = ItemAccount("", "", null, null, null)
        val itemAccount2 = ItemAccount("", "", null, null, null)
        whenever(mockWalletAccountHelper.getHdAccounts(any())).thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any())).thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 2 headers, 3 accounts, 3 legacy addresses
        captor.firstValue.size equals 8
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyRequestTypeSend() {
        // Arrange
        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.SEND)
        val contact0 = Contact()
        contact0.mdid = "mdid"
        val contact1 = Contact()
        contact1.mdid = "mdid"
        val contact2 = Contact()
        whenever(mockContactsDataManager.contactList).thenReturn(Observable.just(contact0, contact1, contact2))
        val itemAccount0 = ItemAccount("", "", null, null, null)
        val itemAccount1 = ItemAccount("", "", null, null, null)
        val itemAccount2 = ItemAccount("", "", null, null, null)
        whenever(mockWalletAccountHelper.getHdAccounts(any())).thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        whenever(mockWalletAccountHelper.getLegacyAddresses(any())).thenReturn(Arrays.asList(itemAccount0, itemAccount1, itemAccount2))
        // Act
        subject.onViewReady()
        // Assert
        verify(mockWalletAccountHelper).getHdAccounts(any())
        verify(mockWalletAccountHelper).getLegacyAddresses(any())
        val captor = argumentCaptor<List<ItemAccount>>()
        verify(mockActivity).updateUi(captor.capture())
        // Value includes 3 headers, 3 accounts, 3 legacy addresses, 2 confirmed contacts
        captor.firstValue.size equals 11
    }

}






