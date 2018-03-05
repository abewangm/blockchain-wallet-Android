package piuk.blockchain.android.ui.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.connectivity.ConnectionEvent;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */
@SuppressLint("Registered")
public class BaseAuthActivity extends AppCompatActivity {

    private static CompositeDisposable compositeDisposable;
    private static Observable<ConnectionEvent> connectionEventObservable;
    private AlertDialog mAlertDialog;
    @Inject protected SSLVerifyUtil mSSLVerifyUtil;
    @Inject protected PrefsUtil mPrefsUtil;
    @Inject protected RxBus rxBus;

    {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lockScreenOrientation();

        compositeDisposable = new CompositeDisposable();

        connectionEventObservable = rxBus.register(ConnectionEvent.class);
        compositeDisposable.add(
                connectionEventObservable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(connectionEvent -> {
                            if (connectionEvent.equals(ConnectionEvent.PINNING_FAIL)) {
                                showAlertDialog(getString(R.string.ssl_pinning_invalid), true);
                            } else {
                                showAlertDialog(getString(R.string.ssl_no_connection), false);
                            }
                        }));
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected void lockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Applies the title to the {@link Toolbar} which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The {@link Toolbar} for the current activity
     * @param title   The title for the page, as a StringRes
     */
    public void setupToolbar(Toolbar toolbar, @StringRes int title) {
        setupToolbar(toolbar, getString(title));
    }

    /**
     * Applies the title to the {@link Toolbar} which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The {@link Toolbar} for the current activity
     * @param title   The title for the page, as a String
     */
    public void setupToolbar(Toolbar toolbar, String title) {
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
    }

    /**
     * Applies the title to the Activity's {@link ActionBar}. This method is the fragment equivalent
     * of {@link #setupToolbar(Toolbar, int)}.
     *
     * @param actionBar The {@link ActionBar} for the current activity
     * @param title     The title for the page, as a StringRes
     */
    public void setupToolbar(ActionBar actionBar, @StringRes int title) {
        setupToolbar(actionBar, getString(title));
    }

    /**
     * Applies the title to the Activity's {@link ActionBar}. This method is the fragment equivalent
     * of {@link #setupToolbar(Toolbar, int)}.
     *
     * @param actionBar The {@link ActionBar} for the current activity
     * @param title     The title for the page, as a String
     */
    public void setupToolbar(ActionBar actionBar, String title) {
        actionBar.setTitle(title);
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        stopLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityResumed();

        if (mPrefsUtil.getValue(PrefsUtil.KEY_SCREENSHOTS_ENABLED, false) && !enforceFlagSecure()) {
            enableScreenshots();
        } else {
            disallowScreenshots();
        }
    }

    /**
     * Allows us to enable screenshots on all pages, unless this is overridden in an Activity and
     * returns true. Some pages are fine to be screenshot, but this lets us keep it permanently
     * disabled on some more sensitive pages.
     *
     * @return False by default. If false, screenshots & screen recording will be allowed on the
     * page if the user so chooses.
     */
    protected boolean enforceFlagSecure() {
        return false;
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        startLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityPaused();
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxBus.unregister(ConnectionEvent.class, connectionEventObservable);
        compositeDisposable.clear();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    /**
     * Init font library by passing in base Context.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected void startLogoutTimer() {
        AccessState.getInstance().startLogoutTimer(this);
    }

    private void stopLogoutTimer() {
        AccessState.getInstance().stopLogoutTimer(this);
    }

    private void disallowScreenshots() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void enableScreenshots() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void showAlertDialog(final String message, final boolean forceExit) {
        if (mAlertDialog != null) mAlertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(message)
                .setCancelable(false);

        if (!forceExit) {
            builder.setPositiveButton(R.string.retry, (d, id) -> mSSLVerifyUtil.validateSSL());
        }

        builder.setNegativeButton(R.string.exit, (d, id) -> finish());

        mAlertDialog = builder.create();
        if (!isFinishing()) {
            mAlertDialog.show();
        }
    }

}
