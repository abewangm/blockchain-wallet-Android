package piuk.blockchain.android.ui.base;

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
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.util.AndroidUtils;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.SSLVerifyUtil;

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */

public class BaseAuthActivity extends AppCompatActivity {

    private AlertDialog mAlertDialog;
    private SSLVerifyUtil mSSLVerifyUtil = new SSLVerifyUtil(this);
    private static CompositeDisposable compositeDisposable;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (!BuildConfig.DOGFOOD && !BuildConfig.DEBUG) {
            disallowScreenshots();
        }

        compositeDisposable = new CompositeDisposable();

        // Subscribe to SSL pinning events
        compositeDisposable.add(
                mSSLVerifyUtil.getSslPinningSubject()
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(sslEvent -> {
                                    switch (sslEvent) {
                                        case ServerDown:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case PinningFail:
                                            showAlertDialog(getString(R.string.ssl_pinning_invalid), true);
                                            break;
                                        case NoConnection:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case Success:
                                            // No-op
                                        default:
                                            // No-op
                                    }
                                },
                                Throwable::printStackTrace));
    }

    /**
     * Applies the title to the {@link Toolbar} which is then set as the Activity's
     * SupportActionBar. Also applies the Montserrat-Regular font, as this cannot be done elsewhere
     * for now.
     *
     * @param toolbar The {@link Toolbar} for the current activity
     * @param title   The title for the page, as a StringRes
     */
    public void setupToolbar(Toolbar toolbar, @StringRes int title) {
        // Fix for bug with formatted ActionBars https://android-review.googlesource.com/#/c/47831/
        if (AndroidUtils.is18orHigher()) {
            toolbar.setTitle(CalligraphyUtils.applyTypefaceSpan(
                    getString(title),
                    TypefaceUtils.load(getAssets(), "fonts/Montserrat-Regular.ttf")));
        }

        setSupportActionBar(toolbar);
    }

    /**
     * Applies the title to the Activity's {@link ActionBar}. This method is the fragment equivalent
     * of {@link #setupToolbar(Toolbar, int)} Also applies the Montserrat-Regular font, as this
     * cannot be done elsewhere for now.
     *
     * @param actionBar The {@link ActionBar} for the current activity
     * @param title     The title for the page, as a StringRes
     */
    public void setupToolbar(ActionBar actionBar, @StringRes int title) {
        // Fix for bug with formatted ActionBars https://android-review.googlesource.com/#/c/47831/
        if (AndroidUtils.is18orHigher()) {
            actionBar.setTitle(CalligraphyUtils.applyTypefaceSpan(
                    getString(title),
                    TypefaceUtils.load(getAssets(), "fonts/Montserrat-Regular.ttf")));
        }
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        stopLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityResumed();
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

    /**
     * Override if you want a particular activity to be able to be screenshot.
     */
    protected void disallowScreenshots() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void showAlertDialog(final String message, final boolean forceExit) {
        if (mAlertDialog != null) mAlertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(message)
                .setCancelable(false);

        if (!forceExit) {
            builder.setPositiveButton(R.string.retry, (d, id) -> {
                // Retry
                mSSLVerifyUtil.validateSSL();
            });
        }

        builder.setNegativeButton(R.string.exit, (d, id) -> finish());

        mAlertDialog = builder.create();

        if (!isFinishing()) {
            mAlertDialog.show();
        }
    }
}
