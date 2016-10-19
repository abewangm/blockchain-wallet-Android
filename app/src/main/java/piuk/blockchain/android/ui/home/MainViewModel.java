package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import info.blockchain.api.DynamicFee;
import info.blockchain.api.ExchangeTicker;
import info.blockchain.api.PersistentUrls;
import info.blockchain.api.Settings;
import info.blockchain.api.Unspent;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.cache.DefaultAccountUnspentCache;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.RootUtil;
import rx.Observable;

@SuppressWarnings("WeakerAccess")
public class MainViewModel extends BaseViewModel {

    private Context context;
    private DataListener dataListener;
    private OSUtil osUtil;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected PayloadManager payloadManager;

    private long mBackPressed;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    public interface DataListener {
        void onRooted();

        void onConnectivityFail();

        void onFetchTransactionsStart();

        void onFetchTransactionCompleted();

        void onScanInput(String strUri);

        void onStartBalanceFragment();

        void onExitConfirmToast();

        void kickToLauncherPage();

        void showEmailVerificationDialog(String email);
    }

    public MainViewModel(Context context, DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.context = context;
        this.dataListener = dataListener;
        osUtil = new OSUtil(context);
        appUtil.applyPRNGFixes();
    }

    @Override
    public void onViewReady() {
        checkBackendEnvironment();
        checkRooted();
        checkConnectivity();
        checkIfShouldShowEmailVerification();
    }

    private void checkIfShouldShowEmailVerification() {
        if (prefs.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
            mCompositeSubscription.add(
                    getSettingsApi()
                            .compose(RxUtil.applySchedulers())
                            .subscribe(settings -> {
                                if (!settings.isEmailVerified()) {
                                    appUtil.setNewlyCreated(false);
                                    String email = settings.getEmail();
                                    dataListener.showEmailVerificationDialog(email);
                                }
                            }, Throwable::printStackTrace));
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
                Looper.loop();
            }).start();

            new Thread(() -> {

                Looper.prepare();

                try {
                    payloadManager.updateBalancesAndTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                dataListener.onFetchTransactionCompleted();

                dataListener.onStartBalanceFragment();

                if (prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").length() > 0) {
                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                    prefs.setValue(PrefsUtil.KEY_SCHEME_URL, "");
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
        stopWebSocketService();
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
        payloadManager.wipe();
        MultiAddrFactory.getInstance().wipe();
        prefs.logOut();
        appUtil.restartApp();
    }

    public void onBackPressed() {
        if (mBackPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(context);
            return;
        } else {
            dataListener.onExitConfirmToast();
        }

        mBackPressed = System.currentTimeMillis();
    }

    public void startWebSocketService() {
        if (!osUtil.isServiceRunning(piuk.blockchain.android.data.websocket.WebSocketService.class)) {
            context.startService(new Intent(context, piuk.blockchain.android.data.websocket.WebSocketService.class));
        }
    }

    public void stopWebSocketService() {
        if (!osUtil.isServiceRunning(piuk.blockchain.android.data.websocket.WebSocketService.class)) {
            context.stopService(new Intent(context, piuk.blockchain.android.data.websocket.WebSocketService.class));
        }
    }

    private void checkBackendEnvironment() {

        PersistentUrls urls = PersistentUrls.getInstance();

//        if (BuildConfig.DOGFOOD || BuildConfig.DEBUG) {
//            PrefsUtil prefsUtil = new PrefsUtil(context);
//            int currentEnvironment = prefsUtil.getValue(PrefsUtil.KEY_BACKEND_ENVIRONMENT, 0);

//            switch (currentEnvironment) {
//                case 0:
        urls.setProductionEnvironment();
//                    break;
//                case 1:
//                    urls.setDevelopmentEnvironment();
//                    break;
//                case 2:
//                    urls.setStagingEnvironment();
//                    break;
//            }
//        }else{
        urls.setProductionEnvironment();
//        }
    }
}
