package piuk.blockchain.android.data.access;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.payload.PayloadManager;

import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.ui.auth.LogoutActivity;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

// TODO: 21/03/2017 Most of this class can be refactored out
public class AccessState {

    private static final long LOGOUT_TIMEOUT_MILLIS = 1000L * 30L;
    public static final String LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT";

    private PrefsUtil prefs;
    private WalletService walletService;
    private AppUtil appUtil;
    private RxBus rxBus;
    private String pin;
    private boolean isLoggedIn = false;
    private PendingIntent logoutPendingIntent;
    private boolean inSepaCountry = false;
    private static AccessState instance;

    public void initAccessState(Context context, PrefsUtil prefs, WalletService walletService, AppUtil appUtil, RxBus rxBus) {
        this.prefs = prefs;
        this.walletService = walletService;
        this.appUtil = appUtil;
        this.rxBus = rxBus;

        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(AccessState.LOGOUT_ACTION);
        logoutPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static AccessState getInstance() {
        if (instance == null)
            instance = new AccessState();
        return instance;
    }

    // TODO: 31/03/2017 Move all of the web calls out of here

    @Deprecated
    public Observable<Boolean> createPin(String password, String passedPin) {
        return createPinObservable(password, passedPin)
                .compose(RxUtil.applySchedulersToObservable());
    }

    @Deprecated
    public Observable<String> validatePin(String passedPin) {
        pin = passedPin;

        String key = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");
        String encryptedPassword = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");

        return walletService.validateAccess(key, passedPin)
                .map(response -> {
                    if (response.isSuccessful()) {
                        String decryptionKey = response.body().getSuccess();

                        return AESUtil.decrypt(encryptedPassword,
                                decryptionKey,
                                AESUtil.PIN_PBKDF2_ITERATIONS);
                    } else {
                        //Invalid PIN
                        throw new InvalidCredentialsException("Validate access failed");
                    }
                });
    }

    @Deprecated
    public Observable<Boolean> syncPayloadToServer() {
        return Observable.fromCallable(() -> PayloadManager.getInstance().save())
                .compose(RxUtil.applySchedulersToObservable());
    }

    @Deprecated
    private Observable<Boolean> createPinObservable(String password, String passedPin) {
        if (passedPin == null || passedPin.equals("0000") || passedPin.length() != 4) {
            return Observable.just(false);
        }

        pin = passedPin;
        appUtil.applyPRNGFixes();

        return Observable.create(subscriber -> {
            byte[] bytes = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            String key = new String(Hex.encode(bytes), "UTF-8");
            random.nextBytes(bytes);
            String value = new String(Hex.encode(bytes), "UTF-8");

            walletService.setAccessKey(key, value, passedPin)
                    .subscribe(response -> {
                        if (response.isSuccessful()) {
                            String encryptionKey = Hex.toHexString(value.getBytes("UTF-8"));

                            String encryptedPassword = new AESUtilWrapper().encrypt(
                                    password, encryptionKey, AESUtil.PIN_PBKDF2_ITERATIONS);

                            prefs.setValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD,
                                    encryptedPassword);
                            prefs.setValue(PrefsUtil.KEY_PIN_IDENTIFIER, key);

                            if (!subscriber.isDisposed()) {
                                subscriber.onNext(true);
                                subscriber.onComplete();
                            }
                        } else {
                            throw Exceptions.propagate(new Throwable("Validate access failed: " + response.errorBody().string()));
                        }

                    }, throwable -> {
                        if (!subscriber.isDisposed()) {
                            subscriber.onNext(false);
                            subscriber.onComplete();
                        }
                    });
        });
    }

    public void setPIN(@Nullable String pin) {
        this.pin = pin;
    }

    public String getPIN() {
        return pin;
    }

    /**
     * Called from {@link BaseAuthActivity#onPause()}
     */
    public void startLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS, logoutPendingIntent);
    }

    /**
     * Called from {@link BaseAuthActivity#onResume()}
     */
    public void stopLogoutTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(logoutPendingIntent);
    }

    public void logout(Context context) {
        pin = null;
        Intent intent = new Intent(context, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction(LOGOUT_ACTION);
        context.startActivity(intent);
    }

    /**
     * Returns whether or not a user is accessing their wallet from a SEPA country, ie should be
     * able to see buy/sell prompts.
     */
    public boolean getInSepaCountry() {
        return inSepaCountry;
    }

    public void setInSepaCountry(boolean inSepaCountry) {
        this.inSepaCountry = inSepaCountry;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean loggedIn) {
        prefs.logIn();
        isLoggedIn = loggedIn;
        if (isLoggedIn) {
            rxBus.emitEvent(AuthEvent.class, AuthEvent.LOGIN);
        } else {
            rxBus.emitEvent(AuthEvent.class, AuthEvent.LOGOUT);
        }
    }

    public void unpairWallet() {
        rxBus.emitEvent(AuthEvent.class, AuthEvent.UNPAIR);
    }

}
