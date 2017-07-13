package piuk.blockchain.android.ui.recover;

import android.app.Application;
import android.content.Intent;

import info.blockchain.wallet.payload.data.Wallet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.createwallet.CreateWalletActivity.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.createwallet.CreateWalletActivity.KEY_INTENT_PASSWORD;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class RecoverFundsViewModelTest {

    private RecoverFundsViewModel mSubject;

    @Mock private RecoverFundsActivity mActivity;
    @Mock private AppUtil mAppUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        mSubject = new RecoverFundsViewModel(mActivity);
    }

    /**
     * Recovery phrase missing, should inform user.
     */
    @Test
    public void onContinueClickedNoRecoveryPhrase() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn(null);
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Recovery phrase is too short to be valid, should inform user.
     */
    @Test
    public void onContinueClickedInvalidRecoveryPhraseLength() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Successful restore. Should take the user to the PIN entry page.
     */
    @Test
    public void onContinueClickedSuccessful() throws Exception {
        // Arrange
        String mnemonic = "all all all all all all all all all all all all";
        when(mActivity.getRecoveryPhrase()).thenReturn(mnemonic);
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity).gotoCredentialsActivity(mnemonic);
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Restore failed, inform the user.
     */
    @Test
    public void onContinueClickedFailed() throws Exception {
        // Arrange
        // TODO: 13/07/2017 isValidMnemonic not testable
        // Act

        // Assert
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @SuppressWarnings({"SyntheticAccessorCall", "PrivateMemberAccessBetweenOuterAndInnerClass"})
    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }
    }

    @SuppressWarnings({"SyntheticAccessorCall", "PrivateMemberAccessBetweenOuterAndInnerClass"})
    private class MockDataManagerModule extends DataManagerModule {
    }

}