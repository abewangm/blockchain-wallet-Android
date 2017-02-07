//package piuk.blockchain.android.ui.chooser
//
//import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import piuk.blockchain.android.BlockchainTestApplication
//import piuk.blockchain.android.BuildConfig
//
//
//@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
//@RunWith(RobolectricTestRunner::class)
//class AccountChooserViewModelTest {
//
//    private lateinit var subject: AccountChooserViewModel
//    private var mockActivity: AccountChooserViewModel.DataListener = mock()
//    private var mockPrefsUtil: PrefsUtil = mock()
//    private var mockWalletAccountHelper: WalletAccountHelper = mock()
//    private var mockStringUtils: StringUtils = mock()
//    private var mockContactsDataManager: ContactsDataManager = mock()
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        subject = AccountChooserViewModel(mockActivity).apply {
//            prefsUtil = mockPrefsUtil
//            walletAccountHelper = mockWalletAccountHelper
//            stringUtils = mockStringUtils
//            contactsDataManager = mockContactsDataManager
//        }
//    }
//
//    @Test(expected = RuntimeException::class)
//    @Throws(Exception::class)
//    fun onViewReadyRequestTypeNull() {
//        // Arrange
//        whenever(mockActivity.paymentRequestType).thenReturn(null)
//        // Act
//        subject.onViewReady()
//        // Assert
//
//    }
//
//    @Throws(Exception::class)
//    fun onViewReadyRequestTypeContact() {
//        // Arrange
//        whenever(mockActivity.paymentRequestType).thenReturn(PaymentRequestType.CONTACT)
//        val contact0 = Contact()
//        contact0.mdid = "mdid"
//        val contact1 = Contact()
//        contact1.mdid = "mdid"
//        val contact2 = Contact()
//        whenever(mockContactsDataManager.contactList).thenReturn(Observable.just(contact0, contact1, contact2))
//        // Act
//        subject.onViewReady()
//        // Assert
//        verify(mockContactsDataManager).contactList
//        val captor = argumentCaptor<List<ItemAccount>>()
//        verify(mockActivity).updateUi(captor.capture())
//        captor.allValues.size equals 2
//        assertEquals(2, captor.allValues.size)
//        assertEquals(3, captor.allValues.size)
//    }
//
//}






