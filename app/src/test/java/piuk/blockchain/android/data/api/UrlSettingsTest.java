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
        when(persistentUrls.getCurrentEnvironment()).thenReturn(STAGING);
        subject = new UrlSettings();
        // Act
        PersistentUrls.Environment value = subject.getCurrentEnvironment();
        // Assert
        verify(persistentUrls).getCurrentEnvironment();
        verify(persistentUrls).setCurrentEnvironment(STAGING);
        assertTrue(STAGING.equals(value));
    }

    @Test
    public void changeEnvironmentStaging() throws Exception {
        // Arrange
        subject = new UrlSettings();
        // Act
        subject.changeEnvironment(STAGING);
        // Assert
        verify(persistentUrls).setProductionEnvironment();
        verify(persistentUrls).setCurrentApiUrl(BuildConfig.STAGING_API_SERVER);
        verify(persistentUrls).setCurrentServerUrl(BuildConfig.STAGING_BASE_SERVER);
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
        verify(persistentUrls).setCurrentApiUrl(BuildConfig.DEV_API_SERVER);
        verify(persistentUrls).setCurrentServerUrl(BuildConfig.DEV_BASE_SERVER);
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