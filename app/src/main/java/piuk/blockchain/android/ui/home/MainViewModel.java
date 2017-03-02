package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.data.Balance;
import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.data.FeeList;
import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.WebUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.cache.DefaultAccountUnspentCache;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.BlockExplorerService;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.EventLogHandler;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import piuk.blockchain.android.util.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class MainViewModel extends BaseViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();

    private DataListener dataListener;
    private OSUtil osUtil;
    private MonetaryUtil monetaryUtil;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected AccessState accessState;
    @Inject protected PayloadManager payloadManager;
    @Inject protected ContactsDataManager contactsDataManager;
    @Inject protected SwipeToReceiveHelper swipeToReceiveHelper;
    @Inject protected NotificationTokenManager notificationTokenManager;
    @Inject protected Context applicationContext;
    @Inject protected StringUtils stringUtils;

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

        void showPaymentMismatchDialog(@StringRes int message);

        void updateCurrentPrice(String price);
    }

    public MainViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
        osUtil = new OSUtil(applicationContext);
        monetaryUtil = new MonetaryUtil(getCurrentBitcoinFormat());
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
        dataListener.showProgressDialog(R.string.contacts_broadcasting_payment);

        compositeDisposable.add(
                // Get contacts
                contactsDataManager.getContactList()
                        // Find contact by MDID
                        .filter(ContactsPredicates.filterByMdid(mdid))
                        // Get FacilitatedTransaction from HashMap
                        .flatMap(contact -> Observable.just(contact.getFacilitatedTransactions().get(facilitatedTxId)))
                        // Check the payment value was appropriate
                        .flatMapCompletable(transaction -> {
                            // Too much sent
                            if (transactionValue > transaction.getIntendedAmount()) {
                                dataListener.showPaymentMismatchDialog(R.string.contacts_too_much_sent);
                                return Completable.complete();
                                // Too little sent
                            } else if (transactionValue < transaction.getIntendedAmount()) {
                                dataListener.showPaymentMismatchDialog(R.string.contacts_too_little_sent);
                                return Completable.complete();
                                // Correct amount sent
                            } else {
                                // Broadcast payment to shared metadata service
                                return contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                                        // Show successfully broadcast
                                        .doOnComplete(() -> dataListener.showBroadcastSuccessDialog())
                                        // Show retry dialog if broadcast failed
                                        .doOnError(throwable -> dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId, transactionValue));
                            }
                        })
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
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

    void storeSwipeReceiveAddresses() {
        swipeToReceiveHelper.updateAndStoreAddresses();
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

    boolean areLauncherShortcutsEnabled() {
        return prefs.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true);
    }

    PayloadManager getPayloadManager() {
        return payloadManager;
    }

    private void subscribeToNotifications() {
        compositeDisposable.add(
                FcmCallbackService.getNotificationSubject()
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> checkForMessages(),
                                Throwable::printStackTrace));
    }

    private void registerNodeForMetaDataService() {
        String uri = null;
        boolean fromNotification = false;

        if (prefs.getValue(PrefsUtil.KEY_METADATA_URI, "").length() > 0) {
            uri = prefs.getValue(PrefsUtil.KEY_METADATA_URI, "");
            prefs.removeValue(PrefsUtil.KEY_METADATA_URI);
        }

        if (prefs.getValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, false)) {
            prefs.removeValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION);
            fromNotification = true;
        }

        final String finalUri = uri;
        if (finalUri != null || fromNotification) dataListener.showProgressDialog(R.string.please_wait);

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
                        .andThen(contactsDataManager.registerMdid())
                        .andThen(contactsDataManager.publishXpub())
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(() -> {
                            if (finalUri != null) {
                                dataListener.onStartContactsActivity(finalUri);
                            } else if (finalFromNotification) {
                                dataListener.onStartContactsActivity(null);
                            } else {
                                checkForMessages();
                            }
                        }, throwable -> {
                            //noinspection StatementWithEmptyBody
                            if (throwable instanceof InvalidCredentialsException) {
                                // Double encrypted and not previously set up, ignore error
                            } else {
                                dataListener.showContactsRegistrationFailure();
                            }
                        }));

        notificationTokenManager.resendNotificationToken();
    }

    private void checkIfShouldShowEmailVerification() {
        if (prefs.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
            compositeDisposable.add(
                    getSettingsApi()
                            .compose(RxUtil.applySchedulersToObservable())
                            .subscribe(call -> {

                                if(call.isSuccessful()) {
                                    Settings settings = call.body();
                                    if (settings.isEmailVerified()) {
                                        appUtil.setNewlyCreated(false);
                                        String email = settings.getEmail();
                                        if (email != null && !email.isEmpty()) {
                                            dataListener.showEmailVerificationDialog(email);
                                        } else {
                                            dataListener.showAddEmailDialog();
                                        }
                                    }
                                }

                            }, Throwable::printStackTrace));
        }
    }

    private Observable<Response<Settings>> getSettingsApi() {
        // FIXME: 28/02/2017
//        return Observable.fromCallable(() -> new SettingsManager(
//                payloadManager.getPayload().getGuid(),
//                payloadManager.getPayload().getSharedKey()).getInfo().execute());
        return Observable.empty();
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
        exchangeRateThread();

        if (AccessState.getInstance().isLoggedIn()) {
            dataListener.onFetchTransactionsStart();

            new Thread(() -> {
                Looper.prepare();
                cacheDynamicFee();
                cacheDefaultAccountUnspentData();
                logEvents();
                Looper.loop();
            }).start();

            new Thread(() -> {

                Looper.prepare();

                storeSwipeReceiveAddresses();

                if (dataListener != null) {
                    dataListener.onFetchTransactionCompleted();
                    dataListener.onStartBalanceFragment(false);
                }

                if (prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").length() > 0) {
                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                    prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                    dataListener.onScanInput(strUri);
                }

                Looper.loop();
            }).start();
        } else {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            dataListener.kickToLauncherPage();
        }
    }

    private void cacheDynamicFee() {
        try {
            Response<FeeList> response = Payment.getDynamicFee().execute();
            if(response.isSuccessful()) {
                DynamicFeeCache.getInstance().setCachedDynamicFee(response.body());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cacheDefaultAccountUnspentData() {

        if (payloadManager.getPayload().getHdWallets() != null) {

            int defaultAccountIndex = payloadManager.getPayload().getHdWallets().get(0).getDefaultAccountIdx();

            Account defaultAccount = payloadManager.getPayload().getHdWallets().get(0).getAccounts().get(defaultAccountIndex);
            String xpub = defaultAccount.getXpub();

            try {
                // TODO: 22/02/2017 quick fix. can be improved
                Response<UnspentOutputs> response = new BlockExplorer(
                    BlockchainFramework.getRetrofitServerInstance(),
                    BlockchainFramework.getApiCode())
                .getUnspentOutputs(Arrays.asList(xpub)).execute();

                if(response.isSuccessful()) {
                    DefaultAccountUnspentCache.getInstance()
                        .setUnspentApiResponse(xpub, response.body());
                } else {
                    Log.e(TAG, "Failed to set DefaultAccountUnspentCache. Might not have free outputs to spend.");
                }

            } catch (IOException e) {
                Log.e(TAG, "cacheDefaultAccountUnspentData: ", e);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        appUtil.deleteQR();
        DynamicFeeCache.getInstance().destroy();
        ExchangeRateFactory.getInstance().stopTicker();
    }

    private void exchangeRateThread() {

        new Thread(() -> {
            Looper.prepare();

            try {
                //Start ticker that will refresh btc exchange rate every 5 min
                ExchangeRateFactory.getInstance().startTicker(
                    () -> dataListener.updateCurrentPrice(getFormattedPriceString()));

            } catch (Exception e) {
                Log.e(TAG, "exchangeRateThread: ", e);
            }

            Looper.loop();

        }).start();
    }

    private String getFormattedPriceString() {
        monetaryUtil.updateUnit(getCurrentBitcoinFormat());
        String fiat = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        double lastPrice = ExchangeRateFactory.getInstance().getLastPrice(fiat);
        String fiatSymbol = ExchangeRateFactory.getInstance().getSymbol(fiat);
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(2);

        switch (getCurrentBitcoinFormat()) {
            case MonetaryUtil.MICRO_BTC:
                return stringUtils.getFormattedString(
                        R.string.current_price_bits,
                        fiatSymbol + format.format(monetaryUtil.getUndenominatedAmount(lastPrice)));
            case MonetaryUtil.MILLI_BTC:
                return stringUtils.getFormattedString(
                        R.string.current_price_millibits,
                        fiatSymbol + format.format(monetaryUtil.getUndenominatedAmount(lastPrice)));
            default:
                return stringUtils.getFormattedString(
                        R.string.current_price_btc,
                        fiatSymbol + format.format(monetaryUtil.getUndenominatedAmount(lastPrice)));
        }
    }

    private int getCurrentBitcoinFormat() {
        return prefs.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
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
        EventLogHandler handler = new EventLogHandler(prefs, WebUtil.getInstance());
        handler.log2ndPwEvent(payloadManager.getPayload().isDoubleEncryption());
        handler.logBackupEvent(payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified());

        try {
            List<String> activeLegacyAddressStrings = PayloadManager.getInstance().getPayload().getLegacyAddressStringList();

            // TODO: 21/02/2017 Quick fix. This should be tested thoroughly
            Call<HashMap<String, Balance>> call = new BlockExplorer(
                BlockchainFramework.getRetrofitServerInstance(),
                BlockchainFramework.getApiCode())
                .getBalance(activeLegacyAddressStrings, BlockExplorer.TX_FILTER_ALL);

            Response<HashMap<String, Balance>> exe = call.execute();

            if(exe.isSuccessful()) {

                for(String address : activeLegacyAddressStrings) {
                    Balance balance = exe.body().get(address);
                    handler.logLegacyEvent(balance.getFinalBalance().longValue() > 0L);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "logEvents: ", e);
        }
    }
}
