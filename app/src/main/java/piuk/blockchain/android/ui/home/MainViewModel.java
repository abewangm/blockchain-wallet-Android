package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import info.blockchain.api.Balance;
import info.blockchain.api.DynamicFee;
import info.blockchain.api.ExchangeTicker;
import info.blockchain.api.Settings;
import info.blockchain.api.Unspent;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.cache.DefaultAccountUnspentCache;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.EventLogHandler;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import piuk.blockchain.android.util.ViewUtils;

@SuppressWarnings("WeakerAccess")
public class MainViewModel extends BaseViewModel {

    private static final String TAG = MainViewModel.class.getSimpleName();

    private Context context;
    private DataListener dataListener;
    private OSUtil osUtil;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected AccessState accessState;
    @Inject protected PayloadManager payloadManager;
    @Inject protected ContactsDataManager contactsDataManager;
    @Inject protected SwipeToReceiveHelper swipeToReceiveHelper;

    public interface DataListener {
        void onRooted();

        void onConnectivityFail();

        void onFetchTransactionsStart();

        void onFetchTransactionCompleted();

        void onScanInput(String strUri);

        void onStartContactsActivity(String data);

        void onStartBalanceFragment();

        void kickToLauncherPage();

        void showEmailVerificationDialog(String email);

        void showAddEmailDialog();

        void showProgressDialog();

        void hideProgressDialog();

        void clearAllDynamicShortcuts();

        void showSurveyPrompt();

        void setMessagesVisibility(@ViewUtils.Visibility int visibility);
    }

    public MainViewModel(Context context, DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.context = context;
        this.dataListener = dataListener;
        osUtil = new OSUtil(context);
    }

    @Override
    public void onViewReady() {
        checkRooted();
        checkConnectivity();
        checkIfShouldShowEmailVerification();
        startWebSocketService();
        registerNodeForMetaDataService();
    }

    private void registerNodeForMetaDataService() {
        // TODO: 28/11/2016 How to handle this if it fails?
        // Might be best to delegate this function to a different manager that
        // can retry the call at a later date

        // TODO: 19/12/2016 Handle second password. Could maybe prompt them on login to enter their
        // password to check for new messages

        String uri = null;

        if (prefs.getValue(PrefsUtil.KEY_METADATA_URI, "").length() > 0) {
            uri = prefs.getValue(PrefsUtil.KEY_METADATA_URI, "");
            prefs.removeValue(PrefsUtil.KEY_METADATA_URI);
        }

        final String finalUri = uri;
        if (finalUri != null) dataListener.showProgressDialog();

        compositeDisposable.add(
                contactsDataManager.initContactsService(null)
                        .doAfterTerminate(() -> dataListener.hideProgressDialog())
                        .subscribe(() -> {
                            if (finalUri != null) {
                                dataListener.onStartContactsActivity(finalUri);
                            } else {
                                compositeDisposable.add(
                                        contactsDataManager.fetchContacts()
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
                                                            dataListener.setMessagesVisibility(messages.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                                                            // Marks all messages as read for debugging purposes
                                                            if (!messages.isEmpty()) {
//                                                                for (Message m : (List<Message>) messages) {
//                                                                    contactsDataManager.markMessageAsRead(m.getId(), true).subscribe(new IgnorableDefaultObserver<>());
//                                                                }
                                                            }
                                                        },
                                                        throwable -> Log.e(TAG, "registerNodeForMetaDataService: ", throwable)));
                            }
                            // TODO: 01/12/2016 Should probably inform the user here if coming from URI click
                        }, throwable -> Log.wtf(TAG, "registerNodeForMetaDataService: ", throwable)));
    }

    public void storeSwipeReceiveAddresses() {
        swipeToReceiveHelper.updateAndStoreAddresses();
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

    public void checkIfShouldShowSurvey() {
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

    private Observable<Settings> getSettingsApi() {
        return Observable.fromCallable(() -> new Settings(payloadManager.getPayload().getGuid(), payloadManager.getPayload().getSharedKey()));
    }

    public PayloadManager getPayloadManager() {
        return payloadManager;
    }

    private void checkRooted() {
        if (new RootUtil().isDeviceRooted() &&
                !prefs.getValue("disable_root_warning", false)) {
            dataListener.onRooted();
        }
    }

    private void checkConnectivity() {
        if (ConnectivityStatus.hasConnectivity(context)) {
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

                try {
                    payloadManager.updateBalancesAndTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                storeSwipeReceiveAddresses();

                if (dataListener != null) {
                    dataListener.onFetchTransactionCompleted();
                    dataListener.onStartBalanceFragment();
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
            DynamicFeeCache.getInstance().setSuggestedFee(new DynamicFee().getDynamicFee());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cacheDefaultAccountUnspentData() {

        if (payloadManager.getPayload().getHdWallet() != null) {

            int defaultAccountIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();

            Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultAccountIndex);
            String xpub = defaultAccount.getXpub();

            try {
                JSONObject unspentResponse = new Unspent().getUnspentOutputs(xpub);
                DefaultAccountUnspentCache.getInstance().setUnspentApiResponse(xpub, unspentResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        appUtil.deleteQR();
        context = null;
        dataListener = null;
        DynamicFeeCache.getInstance().destroy();
    }

    private void exchangeRateThread() {

        List<String> currencies = Arrays.asList(ExchangeRateFactory.getInstance().getCurrencies());
        String strCurrentSelectedFiat = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        if (!currencies.contains(strCurrentSelectedFiat)) {
            prefs.setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        }

        new Thread(() -> {
            Looper.prepare();

            String response = null;
            try {
                // TODO: 07/09/2016 Exchange rate only fetched once per session? Should try to update more often
                response = new ExchangeTicker().getExchangeRate();

                ExchangeRateFactory.getInstance().setData(response);
                ExchangeRateFactory.getInstance().updateFxPricesForEnabledCurrencies();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Looper.loop();

        }).start();
    }

    public void unpair() {
        dataListener.clearAllDynamicShortcuts();
        payloadManager.wipe();
        MultiAddrFactory.getInstance().wipe();
        prefs.logOut();
        appUtil.restartApp();
        accessState.setPIN(null);
    }

    public boolean areLauncherShortcutsEnabled() {
        return prefs.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true);
    }

    private void startWebSocketService() {
        Intent intent = new Intent(context, WebSocketService.class);

        if (!osUtil.isServiceRunning(WebSocketService.class)) {
            context.startService(intent);
        } else {
            // Restarting this here ensures re-subscription after app restart - the service may remain
            // running, but the subscription to the WebSocket won't be restarted unless onCreate called
            context.stopService(intent);
            context.startService(intent);
        }
    }

    private void logEvents() {

        EventLogHandler handler = new EventLogHandler(prefs, WebUtil.getInstance());
        handler.log2ndPwEvent(payloadManager.getPayload().isDoubleEncrypted());
        handler.logBackupEvent(payloadManager.getPayload().getHdWallet().isMnemonicVerified());

        try {
            List<String> activeLegacyAddressStrings = PayloadManager.getInstance().getPayload().getLegacyAddressStringList();
            long balance = new Balance().getTotalBalance(activeLegacyAddressStrings);
            handler.logLegacyEvent(balance > 0L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
