package piuk.blockchain.android.ui.auth

import android.content.Intent
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter
import piuk.blockchain.android.ui.createwallet.CreateWalletView
import piuk.blockchain.android.ui.customviews.ToastCustom

@Config(sdk = intArrayOf(23), constants = BuildConfig::class, application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class CreateWalletPresenterTest {

    private lateinit var subject: CreateWalletPresenter
    private var view: CreateWalletView = mock()

    @Before
    fun setUp() {
        subject = CreateWalletPresenter()
        subject.initView(view)
    }

    @Test
    fun onViewReady() {
        //nothing to test
    }

    @Test
    fun `parseExtras for create`() {
        // Arrange
        val intent = Intent().apply { putExtra(LandingActivity.KEY_INTENT_RECOVERING_FUNDS, false) }

        // Act
        subject.parseExtras(intent)
        // Assert
        verify(view).setTitleText(R.string.new_wallet)
        verify(view).setNextText(R.string.create_wallet)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `parseExtras for recover`() {
        // Arrange
        val intent = Intent().apply { putExtra(LandingActivity.KEY_INTENT_RECOVERING_FUNDS, true) }

        // Act
        subject.parseExtras(intent)
        // Assert
        verify(view).setTitleText(R.string.recover_funds)
        verify(view).setNextText(R.string.dialog_continue)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on weak password`() {
        // Arrange

        // Act
        subject.calculateEntropy("password")
        // Assert
        verify(view).setEntropyStrength(8)
        verify(view).setEntropyLevel(0)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on regular password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyWallet")
        // Assert
        verify(view).setEntropyStrength(46)
        verify(view).setEntropyLevel(1)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on normal password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyTestWallet")
        // Assert
        verify(view).setEntropyStrength(69)
        verify(view).setEntropyLevel(2)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `calculateEntropy on strong password`() {
        // Arrange

        // Act
        subject.calculateEntropy("MyTestWallet!@!ASD@!")
        // Assert
        verify(view).setEntropyStrength(100)
        verify(view).setEntropyLevel(3)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials normal`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john@snow.com", "MyTestWallet","MyTestWallet")
        // Assert
        verify(view).startNextActivity(any(), any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials invalid email`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john", "MyTestWallet","MyTestWallet")
        // Assert
        verify(view).showToast(R.string.invalid_email, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials short_password`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john@snow.com", "aaa","aaa")
        // Assert
        verify(view).showToast(R.string.invalid_password_too_short, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials password_missmatch`() {
        // Arrange

        // Act
        subject.passwordStrength = 80
        subject.validateCredentials("john@snow.com", "MyTestWallet","MyTestWallet2")
        // Assert
        verify(view).showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `validateCredentials weak_password`() {
        // Arrange

        // Act
        subject.passwordStrength = 20
        subject.validateCredentials("john@snow.com", "aaaaaa","aaaaaa")
        // Assert
        verify(view).showWeakPasswordDialog(any(), any())
        verifyNoMoreInteractions(view)
    }
}
