package piuk.blockchain.android;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.FrameworkInterface;
import info.blockchain.wallet.api.WalletApi;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.plugins.RxJavaPlugins;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectivityManager;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;
import piuk.blockchain.android.util.exceptions.LoggingExceptionHandler;
import retrofit2.Retrofit;

/**
 * Created by adambennett on 04/08/2016.
 */

public class BlockchainApplication extends Application implements FrameworkInterface {

    public static final String RX_ERROR_TAG = "RxJava Error";
    @Thunk static final String TAG = BlockchainApplication.class.getSimpleName();

    @Inject
    @Named("api")
    protected Lazy<Retrofit> retrofitApi;
    @Inject
    @Named("server")
    protected Lazy<Retrofit> retrofitServer;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        CalligraphyConfig.initDefault(
                new CalligraphyConfig.Builder()
                        .addCustomStyle(AppCompatButton.class, R.attr.buttonStyle)
                        .setFontAttrId(R.attr.fontPath)
                        .build());

        if (BuildConfig.DEBUG && !AndroidUtils.is21orHigher()) {
            MultiDex.install(base);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Init objects first
        Injector.getInstance().init(this);
        // Inject into Application
        Injector.getInstance().getAppComponent().inject(this);
        // Pass objects to JAR
        BlockchainFramework.init(this);

        new LoggingExceptionHandler();

        RxJavaPlugins.setErrorHandler(throwable -> Log.e(RX_ERROR_TAG, throwable.getMessage(), throwable));

        AppUtil appUtil = new AppUtil(this);

        AccessState.getInstance().initAccessState(this,
                new PrefsUtil(this),
                new WalletService(new WalletApi()),
                appUtil);

        // Apply PRNG fixes on app start if needed
        appUtil.applyPRNGFixes();

        ConnectivityManager.getInstance().registerNetworkListener(this);

        checkSecurityProviderAndPatchIfNeeded();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //noinspection AnonymousInnerClassMayBeStatic
        ApplicationLifeCycle.getInstance().addListener(new ApplicationLifeCycle.LifeCycleListener() {
            @Override
            public void onBecameForeground() {
                // Ensure that PRNG fixes are always current for the session
                appUtil.applyPRNGFixes();
            }

            @Override
            public void onBecameBackground() {
                // No-op
            }
        });
    }

    // Pass instances to JAR Framework, evaluate after object graph instantiated fully
    @Override
    public Retrofit getRetrofitApiInstance() {
        return retrofitApi.get();
    }

    @Override
    public Retrofit getRetrofitServerInstance() {
        return retrofitServer.get();
    }

    @Override
    public Retrofit getRetrofitSFOXInstance() {
        // TODO: 20/03/2017 This will need updating shortly
        return null;
    }

    @Override
    public String getApiCode() {
        return "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3";
    }

    @Override
    public String getDevice() {
        return "android";
    }

    @Override
    public String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * This patches a device's Security Provider asynchronously to help defend against various
     * vulnerabilities. This provider is normally updated in Google Play Services anyway, but this
     * will catch any immediate issues that haven't been fixed in a slow rollout.
     *
     * In the future, we may want to show some kind of warning to users or even stop the app, but
     * this will harm users with versions of Android without GMS approval.
     *
     * @see <a href="https://developer.android.com/training/articles/security-gms-provider.html">Updating
     * Your Security Provider</a>
     */
    protected void checkSecurityProviderAndPatchIfNeeded() {
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
                Log.i(TAG, "Security Provider installed");
            }

            @Override
            public void onProviderInstallFailed(int errorCode, Intent intent) {
                if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
                    showError(errorCode);
                } else {
                    // Google Play services is not available.
                    onProviderInstallerNotAvailable();
                }
            }
        });
    }

    /**
     * Show a dialog prompting the user to install/update/enable Google Play services.
     *
     * @param errorCode Recoverable error code
     */
    @Thunk
    void showError(int errorCode) {
        // TODO: 05/08/2016 Decide if we should alert users here or not
        Log.e(TAG, "Security Provider install failed with recoverable error: " +
                GoogleApiAvailability.getInstance().getErrorString(errorCode));
    }

    /**
     * This is reached if the provider cannot be updated for some reason. App should consider all
     * HTTP communication to be vulnerable, and take appropriate action.
     */
    @Thunk
    void onProviderInstallerNotAvailable() {
        // TODO: 05/08/2016 Decide if we should take action here or not
        Log.wtf(TAG, "Security Provider Installer not available");
    }
}
