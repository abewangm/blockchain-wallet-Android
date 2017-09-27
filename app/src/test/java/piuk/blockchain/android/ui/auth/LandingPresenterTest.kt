package piuk.blockchain.android.ui.auth

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.android.data.datamanagers.PromptManager
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil

class LandingPresenterTest {

    private lateinit var subject: LandingPresenter
    private val mockActivity: LandingView = mock()
    private val mockContext: Context = mock()

    private var appUtil: AppUtil = mock()
    private var environmentSettings: EnvironmentSettings = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private var promptManager: PromptManager = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)

    @Before
    fun setUp() {
        subject = LandingPresenter(
                appUtil,
                environmentSettings,
                promptManager
        )
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady show debug`() {
        //Arrange
        whenever(environmentSettings.shouldShowDebugMenu()).thenReturn(true)
        val environment = Environment.fromString(BuildConfig.ENVIRONMENT)
        whenever(environmentSettings.environment).thenReturn(environment)
        //Act
        subject.onViewReady()
        //Assert
        verify(mockActivity).showToast("Current environment: env_prod", ToastCustom.TYPE_GENERAL)
        verify(mockActivity).showDebugMenu()
    }

    @Test
    @Throws(Exception::class)
    fun `onViewReady no debug`() {
        //Arrange
        whenever(environmentSettings.shouldShowDebugMenu()).thenReturn(false)
        //Act
        subject.onViewReady()
        //Assert
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun initPreLoginPrompts() {
        //Arrange

        //Act
        subject.initPreLoginPrompts(mockContext)
        //Assert
        val testObserver = promptManager.getPreLoginPrompts(mockContext).test()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}
