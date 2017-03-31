package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.payload.PayloadManager;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.contacts.ContactsEvent;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.data.notifications.NotificationPayload;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class MainViewModel extends BaseViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();

    private DataListener dataListener;
    private OSUtil osUtil;
    private Observable<NotificationPayload> notificationObservable;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected AccessState accessState;
    @Inject protected PayloadManager payloadManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected ContactsDataManager contactsDataManager;
    @Inject protected SendDataManager sendDataManager;
    @Inject protected NotificationTokenManager notificationTokenManager;
    @Inject protected Context applicationContext;
    @Inject protected StringUtils stringUtils;
    @Inject protected SettingsDataManager settingsDataManager;
    @Inject protected DynamicFeeCache dynamicFeeCache;
    @Inject protected ExchangeRateFactory exchangeRateFactory;
    @Inject protected RxBus rxBus;

    public interface DataListener {

        /**
         * We can't simply call BuildConfig.CONTACTS_ENABLED in this class as it would make it
         * impossible to test, as it's reliant on the build.gradle config. Passing it here
         * allows us to change the response via mocking the DataListener.
         *
         * TODO: This should be removed once/if Contacts ships
         */
        boolean getIfContactsEnabled();

        void onRooted();

        void onConnectivityFail();

        void onFetchTransactionsStart();

        void onFetchTransactionCompleted();

        void onScanInput(String strUri);

        void onStartContactsActivity(@Nullable String data);

        void onStartBalanceFragment(boolean paymentToContactMade);

        void kickToLauncherPage();

        void showEmailVerificationDialog(String email);

        void showAddEmailDialog();

        void showProgressDialog(@StringRes int message);

        void hideProgressDialog();

        void clearAllDynamicShortcuts();

        void showSurveyPrompt();

        void showContactsRegistrationFailure();

        void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId, long transactionValue);

        void showBroadcastSuccessDialog();

        void updateCurrentPrice(String price);
    }

    public MainViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        osUtil = new OSUtil(applicationContext);
    }

    @Override
    public void onViewReady() {
        checkRooted();
        checkConnectivity();
        checkIfShouldShowEmailVerification();
        startWebSocketService();
        subscribeToNotifications();
        if (dataListener.getIfContactsEnabled()) {
            registerNodeForMetaDataService();
        }
    }

    void broadcastPaymentSuccess(String mdid, String txHash, String facilitatedTxId, long transactionValue) {
        compositeDisposable.add(
                // Get contacts
                contactsDataManager.getContactList()
                        // Find contact by MDID
                        .filter(ContactsPredicates.filterByMdid(mdid))
                        // Get FacilitatedTransaction from HashMap
                        .flatMap(contact -> Observable.just(contact.getFacilitatedTransactions().get(facilitatedTxId)))
                        // Check the payment value was appropriate
                        .flatMapCompletable(transaction -> {
                            // Broadcast payment to shared metadata service
                            return contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                                    // Show successfully broadcast
                                    .doOnComplete(() -> dataListener.showBroadcastSuccessDialog())
                                    // Show retry dialog if broadcast failed
                                    .doOnError(throwable -> dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId, transactionValue));
                        })
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .doOnSubscribe(disposable -> dataListener.showProgressDialog(R.string.contacts_broadcasting_payment))
                        .subscribe(
                                () -> {
                                    // No-op
                                }, throwable -> {
                                    // Not sure if it's worth notifying people at this point? Dialogs are advisory anyway.
                                }));
    }

    void checkForMessages() {
        compositeDisposable.add(
                contactsDataManager.loadNodes()
                        .flatMapCompletable(
                                success -> {
                                    if (success) {
                                        return contactsDataManager.fetchContacts();
                                    } else {
                                        return Completable.error(new Throwable("Nodes not loaded"));
                                    }
                                })
                        .andThen(contactsDataManager.getContactList())
                        .toList()
                        .flatMapObservable(contacts -> {
                            if (!contacts.isEmpty()) {
                                return contactsDataManager.getMessages(true);
                            } else {
                                return Observable.just(Collections.emptyList());
                            }
                        })
                        .subscribe(
                                messages -> {
                                    // No-op
                                },
                                throwable -> Log.e(TAG, "checkForMessages: ", throwable)));
    }

    void checkIfShouldShowSurvey() {
        if (!prefs.getValue(PrefsUtil.KEY_SURVEY_COMPLETED, false)) {
            int visitsToPageThisSession = prefs.getValue(PrefsUtil.KEY_SURVEY_VISITS, 0);
            // Trigger first time coming back to transaction tab
            if (visitsToPageThisSession == 1) {
                // Don't show past June 30th
                Calendar surveyCutoffDate = Calendar.getInstance();
                surveyCutoffDate.set(Calendar.YEAR, 2017);
                surveyCutoffDate.set(Calendar.MONTH, Calendar.JUNE);
                surveyCutoffDate.set(Calendar.DAY_OF_MONTH, 30);

                if (Calendar.getInstance().before(surveyCutoffDate)) {
                    dataListener.showSurveyPrompt();
                    prefs.setValue(PrefsUtil.KEY_SURVEY_COMPLETED, true);
                }
            } else {
                visitsToPageThisSession++;
                prefs.setValue(PrefsUtil.KEY_SURVEY_VISITS, visitsToPageThisSession);
            }
        }

    }

    void unpair() {
        dataListener.clearAllDynamicShortcuts();
        payloadManager.wipe();
        prefs.logOut();
        appUtil.restartApp();
        accessState.setPIN(null);
    }

    PayloadManager getPayloadManager() {
        return payloadManager;
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);

        compositeDisposable.add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> checkForMessages(),
                                Throwable::printStackTrace));
    }

    private void registerNodeForMetaDataService() {
        String uri = null;
        boolean fromNotification = false;

        if (!prefs.getValue(PrefsUtil.KEY_METADATA_URI, "").isEmpty()) {
            uri = prefs.getValue(PrefsUtil.KEY_METADATA_URI, "");
            prefs.removeValue(PrefsUtil.KEY_METADATA_URI);
        }

        if (prefs.getValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, false)) {
            prefs.removeValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION);
            fromNotification = true;
        }

        final String finalUri = uri;
        if (finalUri != null || fromNotification) {
            dataListener.showProgressDialog(R.string.please_wait);
        }

        final boolean finalFromNotification = fromNotification;

        compositeDisposable.add(
                contactsDataManager.loadNodes()
                        .flatMap(loaded -> {
                            if (loaded) {
                                return contactsDataManager.getMetadataNodeFactory();
                            } else {
                                if (!payloadManager.getPayload().isDoubleEncryption()) {
                                    return contactsDataManager.generateNodes(null)
                                            .andThen(contactsDataManager.getMetadataNodeFactory());
                                } else {
                                    throw new InvalidCredentialsException("Payload is double encrypted");
                                }
                            }
                        })
                        .flatMapCompletable(metadataNodeFactory -> contactsDataManager.initContactsService(
                                metadataNodeFactory.getMetadataNode(),
                                metadataNodeFactory.getSharedMetadataNode()))
                        .doOnComplete(() -> rxBus.emitEvent(ContactsEvent.class, ContactsEvent.INIT))
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(
                                () -> registerMdid(finalUri, finalFromNotification),
                                throwable -> {
                                    //noinspection StatementWithEmptyBody
                                    if (throwable instanceof InvalidCredentialsException) {
                                        // Double encrypted and not previously set up, ignore error
                                    } else {
                                        dataListener.showContactsRegistrationFailure();
                                    }
                                }));

        notificationTokenManager.resendNotificationToken();
    }

    // TODO: 30/03/2017 Move this into the registerNodeForMetaDataService function
    private void registerMdid(@Nullable String uri, boolean fromNotification) {
        compositeDisposable.add(
                contactsDataManager.registerMdid()
                        .flatMapCompletable(responseBody -> contactsDataManager.publishXpub())
                        .subscribe(() -> {
                            if (uri != null) {
                                dataListener.onStartContactsActivity(uri);
                            } else if (fromNotification) {
                                dataListener.onStartContactsActivity(null);
                            } else {
                                checkForMessages();
                            }
                        }, throwable -> dataListener.showContactsRegistrationFailure()));
    }

    private void checkIfShouldShowEmailVerification() {
        if (prefs.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
            compositeDisposable.add(
                    getSettingsApi()
                            .compose(RxUtil.applySchedulersToObservable())
                            .subscribe(settings -> {
                                if (!settings.isEmailVerified()) {
                                    appUtil.setNewlyCreated(false);
                                    String email = settings.getEmail();
                                    if (email != null && !email.isEmpty()) {
                                        dataListener.showEmailVerificationDialog(email);
                                    } else {
                                        dataListener.showAddEmailDialog();
                                    }
                                }
                            }, Throwable::printStackTrace));
        }
    }

    private Observable<Settings> getSettingsApi() {
        return settingsDataManager.initSettings(
                payloadManager.getPayload().getGuid(),
                payloadManager.getPayload().getSharedKey());
    }

    private void checkRooted() {
        if (new RootUtil().isDeviceRooted() &&
                !prefs.getValue("disable_root_warning", false)) {
            dataListener.onRooted();
        }
    }

    private void checkConnectivity() {
        if (ConnectivityStatus.hasConnectivity(applicationContext)) {
            preLaunchChecks();
        } else {
            dataListener.onConnectivityFail();
        }
    }

    private void preLaunchChecks() {
        if (accessState.isLoggedIn()) {
            dataListener.onStartBalanceFragment(false);
            dataListener.onFetchTransactionsStart();

            compositeDisposable.add(
                    Completable.fromCallable(() -> {
                        cacheDynamicFee();
                        logEvents();
                        return Void.TYPE;
                    }).compose(RxUtil.applySchedulersToCompletable())
                            .subscribe(() -> {
                                if (dataListener != null) {
                                    dataListener.onFetchTransactionCompleted();
                                }

                                if (!prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").isEmpty()) {
                                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                                    prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                                    dataListener.onScanInput(strUri);
                                }
                            }));
        } else {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            dataListener.kickToLauncherPage();
        }
    }

    private void cacheDynamicFee() {
        compositeDisposable.add(
                sendDataManager.getSuggestedFee()
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(feeList -> dynamicFeeCache.setCachedDynamicFee(feeList),
                                Throwable::printStackTrace));
    }

    @Override
    public void destroy() {
        super.destroy();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        appUtil.deleteQR();
        dynamicFeeCache.destroy();
    }

    public void updateTicker() {
        compositeDisposable.add(exchangeRateFactory.updateTicker().subscribe(() ->
                        dataListener.updateCurrentPrice(getFormattedPriceString()),
                Throwable::printStackTrace));
    }

    private String getFormattedPriceString() {
        String fiat = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        double lastPrice = exchangeRateFactory.getLastPrice(fiat);
        String fiatSymbol = exchangeRateFactory.getSymbol(fiat);
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(2);
        return stringUtils.getFormattedString(
                R.string.current_price_btc,
                fiatSymbol + format.format(lastPrice));
    }

    private void startWebSocketService() {
        Intent intent = new Intent(applicationContext, WebSocketService.class);

        if (!osUtil.isServiceRunning(WebSocketService.class)) {
            applicationContext.startService(intent);
        } else {
            // Restarting this here ensures re-subscription after app restart - the service may remain
            // running, but the subscription to the WebSocket won't be restarted unless onCreate called
            applicationContext.stopService(intent);
            applicationContext.startService(intent);
        }
    }

    private void logEvents() {
        EventService handler = new EventService(prefs, new WalletApi());
        handler.log2ndPwEvent(payloadManager.getPayload().isDoubleEncryption());
        handler.logBackupEvent(payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified());

        try {
            BigInteger importedAddressesBalance = payloadManager.getImportedAddressesBalance();
            if (importedAddressesBalance != null) {
                handler.logLegacyEvent(importedAddressesBalance.longValue() > 0L);
            }
        } catch (Exception e) {
            Log.e(TAG, "logEvents: ", e);
        }
    }

}
