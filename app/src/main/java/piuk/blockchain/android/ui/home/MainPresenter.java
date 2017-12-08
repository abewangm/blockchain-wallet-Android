package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.ethereum.EthereumWallet;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.prices.data.PriceDatum;

import org.bitcoinj.crypto.DeterministicKey;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.auth.AuthService;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.models.ContactsEvent;
import piuk.blockchain.android.data.currency.CryptoCurrencies;
import piuk.blockchain.android.data.currency.CurrencyState;
import piuk.blockchain.android.data.datamanagers.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.PromptManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.exchange.BuyDataManager;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.models.MetadataEvent;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;

public class MainPresenter extends BasePresenter<MainView> {

    private OSUtil osUtil;
    private SwipeToReceiveHelper swipeToReceiveHelper;
    private Observable<NotificationPayload> notificationObservable;
    private PrefsUtil prefs;
    private AppUtil appUtil;
    private AccessState accessState;
    private PayloadManager payloadManager;
    private PayloadDataManager payloadDataManager;
    private ContactsDataManager contactsDataManager;
    private Context applicationContext;
    private StringUtils stringUtils;
    private SettingsDataManager settingsDataManager;
    private BuyDataManager buyDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private ExchangeRateFactory exchangeRateFactory;
    private RxBus rxBus;
    private FeeDataManager feeDataManager;
    private EnvironmentSettings environmentSettings;
    private PromptManager promptManager;
    private EthDataManager ethDataManager;
    private CurrencyState currencyState;
    private WalletOptionsDataManager walletOptionsDataManager;

    @Inject
    MainPresenter(PrefsUtil prefs,
                  AppUtil appUtil,
                  AccessState accessState,
                  PayloadManager payloadManager,
                  PayloadDataManager payloadDataManager,
                  ContactsDataManager contactsDataManager,
                  Context applicationContext,
                  StringUtils stringUtils,
                  SettingsDataManager settingsDataManager,
                  BuyDataManager buyDataManager,
                  DynamicFeeCache dynamicFeeCache,
                  ExchangeRateFactory exchangeRateFactory,
                  RxBus rxBus,
                  FeeDataManager feeDataManager,
                  EnvironmentSettings environmentSettings,
                  PromptManager promptManager,
                  EthDataManager ethDataManager,
                  SwipeToReceiveHelper swipeToReceiveHelper,
                  CurrencyState currencyState,
                  WalletOptionsDataManager walletOptionsDataManager) {

        this.prefs = prefs;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
        this.payloadDataManager = payloadDataManager;
        this.contactsDataManager = contactsDataManager;
        this.applicationContext = applicationContext;
        this.stringUtils = stringUtils;
        this.settingsDataManager = settingsDataManager;
        this.buyDataManager = buyDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
        this.exchangeRateFactory = exchangeRateFactory;
        this.rxBus = rxBus;
        this.feeDataManager = feeDataManager;
        this.environmentSettings = environmentSettings;
        this.promptManager = promptManager;
        this.ethDataManager = ethDataManager;
        this.swipeToReceiveHelper = swipeToReceiveHelper;
        this.currencyState = currencyState;
        this.walletOptionsDataManager = walletOptionsDataManager;
        osUtil = new OSUtil(applicationContext);
    }

    private void initPrompts(Context context) {
        settingsDataManager.getSettings()
                .flatMap(settings -> promptManager.getCustomPrompts(context, settings))
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap(Observable::fromIterable)
                .firstOrError()
                .subscribe(
                        getView()::showCustomPrompt,
                        Timber::e);
    }

    @Override
    public void onViewReady() {
        if (!accessState.isLoggedIn()) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            getView().kickToLauncherPage();
        } else {

            startWebSocketService();
            logEvents();

            getView().showProgressDialog(R.string.please_wait);

            initMetadataElements();

            doWalletOptionsChecks();
        }
    }

    /*
    // TODO: 24/10/2017  WalletOptions api is also accessed in BuyDataManager - This should be improved soon.
     */
    private void doWalletOptionsChecks() {
        walletOptionsDataManager.showShapeshift(
                payloadDataManager.getWallet().getGuid(),
                payloadDataManager.getWallet().getSharedKey())
                .doOnNext(this::setShapeShiftVisibility)
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(ignored -> {
                    //no-op
                }, throwable -> {
                    //Couldn't retrieve wallet options. Not safe to continue
                    Timber.e(throwable);
                    getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
                    accessState.logout(applicationContext);
                });
    }

    @SuppressWarnings("SameParameterValue")
    private void setShapeShiftVisibility(boolean showShapeshift) {
        if (showShapeshift) {
            getView().showShapeshift();
        } else {
            getView().hideShapeshift();
        }
    }

    // Could be used in the future
    @SuppressWarnings("unused")
    private SecurityPromptDialog getWarningPrompt(String message) {
        SecurityPromptDialog prompt = SecurityPromptDialog.newInstance(
                R.string.warning,
                message,
                R.drawable.vector_warning,
                R.string.ok_cap,
                false,
                false);
        prompt.setPositiveButtonListener(view -> prompt.dismiss());
        return prompt;
    }

    void initMetadataElements() {
        initMetadataNodesObservable()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap(metadataNodeFactory -> ethWalletObservable(metadataNodeFactory.getMetadataNode()))
                .flatMapCompletable(metadataNodeFactory -> {
                    //Initialise contacts
                    //contactsDataManager.initContactsService(metadataNodeFactory.getMetadataNode(), metadataNodeFactory.getSharedMetadataNode());
                    //payloadDataManager.registerMdid()
                    //contactsDataManager.publishXpub()
                    return Completable.complete();
                })
                .andThen(feesCompletable())
                .doAfterTerminate(() -> {
                            getView().hideProgressDialog();

                            initPrompts(getView().getActivityContext());
                            storeSwipeReceiveAddresses();

                            rxBus.emitEvent(MetadataEvent.class, MetadataEvent.SETUP_COMPLETE);

                            if (!prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").isEmpty()) {
                                String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                                prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                                getView().onScanInput(strUri);
                            }
                        }
                )
                .subscribe(o -> {
                    if (getView().isBuySellPermitted()) {
                        initBuyService();
                    } else {
                        getView().setBuySellEnabled(false);
                    }
//                    initContactsService();
                }, throwable -> {
                    //noinspection StatementWithEmptyBody
                    if (throwable instanceof InvalidCredentialsException || throwable instanceof HDWalletException) {
                        if (payloadDataManager.isDoubleEncrypted()) {
                            // Wallet double encrypted and needs to be decrypted to set up ether wallet, contacts etc
                            getView().showSecondPasswordDialog();
                        } else {
                            logException(throwable);
                        }
                    } else {
                        logException(throwable);
                    }
                });
    }

    private void logException(Throwable throwable) {
        Logging.INSTANCE.logException(throwable);
        getView().showMetadataNodeFailure();
    }

    private void storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable(() -> {
            swipeToReceiveHelper.updateAndStoreBitcoinAddresses();
            return Void.TYPE;
        }).subscribeOn(Schedulers.computation())
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(() -> { /* No-op*/ }, Timber::e);
    }

    private Observable<MetadataNodeFactory> initMetadataNodesObservable() {
        return payloadDataManager.loadNodes()
                .flatMap(loaded -> {
                    if (loaded) {
                        return payloadDataManager.getMetadataNodeFactory();
                    } else {
                        if (!payloadManager.getPayload().isDoubleEncryption()) {
                            return payloadDataManager.generateNodes(null)
                                    .andThen(payloadDataManager.getMetadataNodeFactory());
                        } else {
                            throw new InvalidCredentialsException("Payload is double encrypted");
                        }
                    }
                });
    }

    private Observable<Map<String, PriceDatum>> feesCompletable() {
        return feeDataManager.getBtcFeeOptions()
                .doOnNext(btcFeeOptions -> dynamicFeeCache.setBtcFeeOptions(btcFeeOptions))
                .flatMap(ignored -> feeDataManager.getEthFeeOptions())
                .doOnNext(ethFeeOptions -> dynamicFeeCache.setEthFeeOptions(ethFeeOptions))
                .compose(RxUtil.applySchedulersToObservable())
                .flatMap(feeOptions -> exchangeRateFactory.updateTickers());
    }

    private void checkForMessages() {
        getCompositeDisposable().add(contactsDataManager.fetchContacts()
                .andThen(contactsDataManager.getContactList())
                .toList()
                .flatMapObservable(contacts -> {
                    if (!contacts.isEmpty()) {
                        return contactsDataManager.getMessages(true);
                    } else {
                        return Observable.just(Collections.emptyList());
                    }
                })
                .subscribe(messages -> {
                    // No-op
                }, Timber::e));
    }

    void unPair() {
        getView().clearAllDynamicShortcuts();
        payloadManager.wipe();
        accessState.logout(applicationContext);
        accessState.unpairWallet();
        appUtil.restartApp();
        accessState.setPIN(null);
        buyDataManager.wipe();
        ethDataManager.clearEthAccountDetails();
    }

    PayloadManager getPayloadManager() {
        return payloadManager;
    }

    // Usage commented out for now, until Contacts is back again
    @SuppressWarnings("unused")
    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);

        getCompositeDisposable().add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> checkForMessages(),
                                Throwable::printStackTrace));
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        appUtil.deleteQR();
        dismissAnnouncementIfOnboardingCompleted();
    }

    void updateTicker() {
        getCompositeDisposable().add(
                exchangeRateFactory.updateTickers()
                        .subscribe(
                                o -> { /* No-op */ },
                                Throwable::printStackTrace));
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
        EventService handler = new EventService(prefs, new AuthService(new WalletApi()));
        handler.log2ndPwEvent(payloadManager.getPayload().isDoubleEncryption());
        handler.logBackupEvent(payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified());

        try {
            BigInteger importedAddressesBalance = payloadManager.getImportedAddressesBalance();
            if (importedAddressesBalance != null) {
                handler.logLegacyEvent(importedAddressesBalance.longValue() > 0L);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    String getCurrentServerUrl() {
        return environmentSettings.getExplorerUrl();
    }

    // Usage commented out for now
    @SuppressWarnings("unused")
    private void initContactsService() {
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
            getView().showProgressDialog(R.string.please_wait);
        }

        rxBus.emitEvent(ContactsEvent.class, ContactsEvent.INIT);

        if (uri != null) {
            getView().onStartContactsActivity(uri);
        } else if (fromNotification) {
            getView().onStartContactsActivity(null);
        } else {
            checkForMessages();
        }
    }

    private void initBuyService() {
        getCompositeDisposable().add(
                buyDataManager.getCanBuy()
                        .subscribe(isEnabled -> {
                            getView().setBuySellEnabled(isEnabled);
                            if (isEnabled) {
                                buyDataManager.watchPendingTrades()
                                        .compose(RxUtil.applySchedulersToObservable())
                                        .subscribe(getView()::onTradeCompleted, Throwable::printStackTrace);

                                buyDataManager.getWebViewLoginDetails()
                                        .subscribe(getView()::setWebViewLoginDetails, Throwable::printStackTrace);
                            }
                        }, throwable -> {
                            Timber.e(throwable);
                            getView().setBuySellEnabled(false);
                        }));
    }

    private void dismissAnnouncementIfOnboardingCompleted() {
        if (prefs.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
                && prefs.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, false)) {
            prefs.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true);
        }
    }

    /**
     * Initialises ethereum wallet.
     */
    private Observable<EthereumWallet> ethWalletObservable(DeterministicKey hdNode) {
        return ethDataManager.initEthereumWallet(hdNode,
                stringUtils.getString(R.string.eth_default_account_label));
    }

    void generateMetadataHDNodeAndEthereumWallet(@Nullable String secondPassword) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().showSecondPasswordDialog();
        } else {
            payloadDataManager.generateNodes(secondPassword)
                    .compose(RxUtil.addCompletableToCompositeDisposable(this))
                    .andThen(payloadDataManager.getMetadataNodeFactory())
                    .flatMap(metadataNodeFactory -> ethWalletObservable(metadataNodeFactory.getMetadataNode()))
                    .subscribe(
                            ethereumWallet -> appUtil.restartApp(),
                            Throwable::printStackTrace);
        }
    }

    CryptoCurrencies getCurrentCryptoCurrency() {
        return currencyState.getCryptoCurrency();
    }

    void setCryptoCurrency(CryptoCurrencies cryptoCurrency) {
        currencyState.setCryptoCurrency(cryptoCurrency);
    }

}
