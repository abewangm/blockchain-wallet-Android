package piuk.blockchain.android.data.api;

import android.app.Application;

import info.blockchain.api.PersistentUrls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

import static info.blockchain.api.PersistentUrls.Environment.DEV;
import static info.blockchain.api.PersistentUrls.Environment.PRODUCTION;
import static info.blockchain.api.PersistentUrls.Environment.STAGING;
import static info.blockchain.api.PersistentUrls.KEY_ENV_PROD;
import static info.blockchain.api.PersistentUrls.KEY_ENV_STAGING;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.data.api.UrlSettings.KEY_CURRENT_ENVIRONMENT;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class UrlSettingsTest {

    private UrlSettings subject;
    @Mock PrefsUtil prefsUtil;
    @Mock AppUtil appUtil;
    @Mock PersistentUrls persistentUrls;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new DataManagerModule());
    }

    @Test
    public void shouldShowDebugMenu() throws Exception {
        // Arrange
        subject = new UrlSettings();
        // Act
        boolean value = subject.shouldShowDebugMenu();
        // Assert
        assertTrue(value);
    }

    @Test
    public void getCurrentEnvironmentDefault() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD)).thenReturn("");
        when(persistentUrls.getCurrentEnvironment()).thenReturn(PersistentUrls.Environment.PRODUCTION);
        subject = new UrlSettings();
        // Act
        PersistentUrls.Environment value = subject.getCurrentEnvironment();
        // Assert
        verify(persistentUrls).getCurrentEnvironment();
        assertTrue(PRODUCTION.equals(value));
    }

    @Test
    public void getCurrentEnvironmentStaging() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_CURRENT_ENVIRONMENT, KEY_ENV_PROD)).thenReturn(KEY_ENV_STAGING);
        subject = new UrlSettings();
        // Act
        PersistentUrls.Environment value = subject.getCurrentEnvironment();
        // Assert
        assertTrue(PersistentUrls.Environment.STAGING.equals(value));
    }

    @Test
    public void changeEnvironmentStaging() throws Exception {
        // Arrange
        subject = new UrlSettings();
        // Act
        subject.changeEnvironment(STAGING);
        // Assert
        verify(persistentUrls).setProductionEnvironment();
        verify(persistentUrls).setAddressInfoUrl(BuildConfig.STAGING_ADDRESS_INFO);
        verify(persistentUrls).setBalanceUrl(BuildConfig.STAGING_BALANCE);
        verify(persistentUrls).setDynamicFeeUrl(BuildConfig.STAGING_DYNAMIC_FEE);
        verify(persistentUrls).setMultiAddressUrl(BuildConfig.STAGING_MULTIADDR_URL);
        verify(persistentUrls).setPinstoreUrl(BuildConfig.STAGING_PIN_STORE_URL);
        verify(persistentUrls).setSettingsUrl(BuildConfig.STAGING_SETTINGS_PAYLOAD_URL);
        verify(persistentUrls).setTransactionDetailsUrl(BuildConfig.STAGING_TRANSACTION_URL);
        verify(persistentUrls).setUnspentUrl(BuildConfig.STAGING_UNSPENT_OUTPUTS_URL);
        verify(persistentUrls).setWalletPayloadUrl(BuildConfig.STAGING_WALLET_PAYLOAD_URL);
        verify(persistentUrls).setCurrentEnvironment(STAGING);
        verify(appUtil).clearCredentialsAndKeepEnvironment();
    }

    @Test
    public void changeEnvironmentDev() throws Exception {
        // Arrange
        subject = new UrlSettings();
        // Act
        subject.changeEnvironment(DEV);
        // Assert
        verify(persistentUrls).setProductionEnvironment();
        verify(persistentUrls).setAddressInfoUrl(BuildConfig.DEV_ADDRESS_INFO);
        verify(persistentUrls).setBalanceUrl(BuildConfig.DEV_BALANCE);
        verify(persistentUrls).setDynamicFeeUrl(BuildConfig.DEV_DYNAMIC_FEE);
        verify(persistentUrls).setMultiAddressUrl(BuildConfig.DEV_MULTIADDR_URL);
        verify(persistentUrls).setPinstoreUrl(BuildConfig.DEV_PIN_STORE_URL);
        verify(persistentUrls).setSettingsUrl(BuildConfig.DEV_SETTINGS_PAYLOAD_URL);
        verify(persistentUrls).setTransactionDetailsUrl(BuildConfig.DEV_TRANSACTION_URL);
        verify(persistentUrls).setUnspentUrl(BuildConfig.DEV_UNSPENT_OUTPUTS_URL);
        verify(persistentUrls).setWalletPayloadUrl(BuildConfig.DEV_WALLET_PAYLOAD_URL);
        verify(persistentUrls).setCurrentEnvironment(DEV);
        verify(appUtil).clearCredentialsAndKeepEnvironment();
    }

    private class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }

        @Override
        protected PersistentUrls providePersistentUrls() {
            return persistentUrls;
        }
    }
}