package piuk.blockchain.android.ui.settings;

import android.content.Context;

import info.blockchain.wallet.util.CharSequenceX;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.PrefsUtil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class SettingsViewModelTest {

    private SettingsViewModel subject;
    @Mock SettingsViewModel.DataListener activity;
    @Mock FingerprintHelper fingerprintHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new ApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        subject = new SettingsViewModel(activity);
    }

    @After
    public void tearDown() throws Exception {
        Mockito.validateMockitoUsage();
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        subject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @Test
    public void getIfFingerprintHardwareAvailable() throws Exception {
        // Arrange
        when(fingerprintHelper.isHardwareDetected()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintHardwareAvailable();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getIfFingerprintUnlockEnabled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintUnlockEnabled();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void setFingerprintUnlockEnabled() throws Exception {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(false);
        // Assert
        verify(fingerprintHelper).setFingerprintUnlockEnabled(false);
        verify(fingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
    }

    @Test
    public void onFingerprintClickedAlreadyEnabled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showDisableFingerprintDialog();
    }

    @Test
    public void onFingerprintClickedNoFingerprintsEnrolled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(false);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showNoFingerprintsAddedDialog();
    }

    @Test
    public void onFingerprintClickedVerifyPin() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).verifyPinCode();
    }

    @Test
    public void pinCodeValidated() throws Exception {
        // Arrange
        CharSequenceX pincode = new CharSequenceX("");
        // Act
        subject.pinCodeValidated(pincode);
        // Assert
        verify(activity).showFingerprintDialog(pincode, fingerprintHelper);
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected FingerprintHelper provideFingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
            return fingerprintHelper;
        }
    }
}