package piuk.blockchain.android.ui.home;

import android.content.Context;
import android.support.annotation.Nullable;

import org.bitcoinj.crypto.DeterministicKey;

import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.prices.data.PriceDatum;

import java.math.BigInteger;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.answers.BitcoinUnits;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.auth.AuthService;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.models.ContactsEvent;
import piuk.blockchain.android.data.currency.CryptoCurrencies;
import piuk.blockchain.android.data.currency.CurrencyState;
import piuk.blockchain.android.data.datamanagers.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.PromptManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.exchange.BuyDataManager;
import piuk.blockchain.android.data.metadata.MetadataManager;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.EventService;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.data.shapeshift.ShapeShiftDataManager;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.dashboard.DashboardPresenter;
import piuk.blockchain.android.ui.home.models.MetadataEvent;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import timber.log.Timber;

public class MainPresenter extends BasePresenter<MainView> {

    private Observable<NotificationPayload> notificationObservable;
    private PrefsUtil prefs;
    private AppUtil appUtil;
    private AccessState accessState;
    private PayloadManager payloadManager;
    private PayloadDataManager payloadDataManager;
    private ContactsDataManager contactsDataManager;
    private Context applicationContext;
    private SettingsDataManager settingsDataManager;
    private BuyDataManager buyDataManager;
    private DynamicFeeCache dynamicFeeCache;
    private ExchangeRateFactory exchangeRateFactory;
    private RxBus rxBus;
    private FeeDataManager feeDataManager;
    private PromptManager promptManager;
    private EthDataManager ethDataManager;
    private BchDataManager bchDataManager;
    private CurrencyState currencyState;
    private WalletOptionsDataManager walletOptionsDataManager;
    private MetadataManager metadataManager;
    private StringUtils stringUtils;
    private ShapeShiftDataManager shapeShiftDataManager;
    private EnvironmentSettings environmentSettings;

    @Inject
    MainPresenter(PrefsUtil prefs,
                  AppUtil appUtil,
                  AccessState accessState,
                  PayloadManager payloadManager,
                  PayloadDataManager payloadDataManager,
                  ContactsDataManager contactsDataManager,
                  Context applicationContext,
                  SettingsDataManager settingsDataManager,
                  BuyDataManager buyDataManager,
                  DynamicFeeCache dynamicFeeCache,
                  ExchangeRateFactory exchangeRateFactory,
                  RxBus rxBus,
                  FeeDataManager feeDataManager,
                  PromptManager promptManager,
                  EthDataManager ethDataManager,
                  BchDataManager bchDataManager,
                  CurrencyState currencyState,
                  WalletOptionsDataManager walletOptionsDataManager,
                  MetadataManager metadataManager,
                  StringUtils stringUtils,
                  ShapeShiftDataManager shapeShiftDataManager,
                  EnvironmentSettings environmentSettings) {

        this.prefs = prefs;
        this.appUtil = appUtil;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
        this.payloadDataManager = payloadDataManager;
        this.contactsDataManager = contactsDataManager;
        this.applicationContext = applicationContext;
        this.settingsDataManager = settingsDataManager;
        this.buyDataManager = buyDataManager;
        this.dynamicFeeCache = dynamicFeeCache;
        this.exchangeRateFactory = exchangeRateFactory;
        this.rxBus = rxBus;
        this.feeDataManager = feeDataManager;
        this.promptManager = promptManager;
        this.ethDataManager = ethDataManager;
        this.bchDataManager = bchDataManager;
        this.currencyState = currencyState;
        this.walletOptionsDataManager = walletOptionsDataManager;
        this.metadataManager = metadataManager;
        this.stringUtils = stringUtils;
        this.shapeShiftDataManager = shapeShiftDataManager;
        this.environmentSettings = environmentSettings;
    }

    private void initPrompts(Context context) {
        settingsDataManager.getSettings()
                .flatMap(settings -> promptManager.getCustomPrompts(context, settings))
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .flatMap(Observable::fromIterable)
                .firstOrError()
                .subscribe(
                        getView()::showCustomPrompt,
                        throwable -> {
                            if (!(throwable instanceof NoSuchElementException)) {
                                Timber.e(throwable);
                            }
                        });
    }

    @Override
    public void onViewReady() {
        if (!accessState.isLoggedIn()) {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            getView().kickToLauncherPage();
        } else {
            logEvents();

            getView().showProgressDialog(R.string.please_wait);

            initMetadataElements();

            doWalletOptionsChecks();

            doPushNotifications();
        }

        Logging.INSTANCE.logCustom(new BitcoinUnits(prefs.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)));
    }

    /**
     * Initial setup of push notifications.
     * We don't subscribe to addresses for notifications when creating a new wallet.
     * To accommodate existing wallets we need subscribe to the next available addresses.
     */
    private void doPushNotifications() {
        if (!prefs.has(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED)) {
            prefs.setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        }

        if (prefs.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)) {
            payloadDataManager.syncPayloadAndPublicKeys()
                    .compose(RxUtil.addCompletableToCompositeDisposable(this))
                    .subscribe(() -> {
                        //no-op
                    }, throwable -> Timber.e(throwable));
        }
    }

    public void doTestnetCheck() {
        if (environmentSettings.getEnvironment().equals(Environment.TESTNET)) {
            currencyState.setCryptoCurrency(CryptoCurrencies.BTC);
            getView().showTestnetWarning();
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
        metadataManager.attemptMetadataSetup()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .andThen(ethCompletable())
                .andThen(shapeshiftCompletable())
                .andThen(bchCompletable())
                .andThen(feesCompletable())
                .doAfterTerminate(() -> {
                            getView().hideProgressDialog();

                            initPrompts(getView().getActivityContext());

                            if (!prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").isEmpty()) {
                                String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                                prefs.removeValue(PrefsUtil.KEY_SCHEME_URL);
                                getView().onScanInput(strUri);
                            }
                        }
                )
                .subscribe(ignore -> {
                    if (getView().isBuySellPermitted()) {
                        initBuyService();
                    } else {
                        getView().setBuySellEnabled(false);
                    }

                    rxBus.emitEvent(MetadataEvent.class, MetadataEvent.SETUP_COMPLETE);
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

    private Completable bchCompletable() {
        return bchDataManager.initBchWallet(stringUtils.getString(R.string.bch_default_account_label))
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load bch wallet");
                });
    }

    private Completable ethCompletable() {
        return ethDataManager.initEthereumWallet(
                stringUtils.getString(R.string.eth_default_account_label))
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load eth wallet");
                });
    }

    private Completable shapeshiftCompletable() {
        return shapeShiftDataManager.initShapeshiftTradeData()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .doOnError(throwable -> {
                    // TODO: 21/02/2018 Reload or disable?
                    Timber.e("Failed to load shape shift trades");
                });
    }

    private void logException(Throwable throwable) {
        Logging.INSTANCE.logException(throwable);
        getView().showMetadataNodeFailure();
    }

    private Observable<Map<String, PriceDatum>> feesCompletable() {
        return feeDataManager.getBtcFeeOptions()
                .doOnNext(btcFeeOptions -> dynamicFeeCache.setBtcFeeOptions(btcFeeOptions))
                .flatMap(ignored -> feeDataManager.getEthFeeOptions())
                .doOnNext(ethFeeOptions -> dynamicFeeCache.setEthFeeOptions(ethFeeOptions))
                .flatMap(ignored -> feeDataManager.getBchFeeOptions())
                .doOnNext(bchFeeOptions -> dynamicFeeCache.setBchFeeOptions(bchFeeOptions))
                .flatMap(feeOptions -> exchangeRateFactory.updateTickers())
                .compose(RxUtil.applySchedulersToObservable())
                .compose(RxUtil.addObservableToCompositeDisposable(this));
    }

    private void checkForMessages() {
        // TODO: 28/02/2018 There is no point in doing this currently
//        getCompositeDisposable().add(contactsDataManager.fetchContacts()
//                .andThen(contactsDataManager.getContactList())
//                .toList()
//                .flatMapObservable(contacts -> {
//                    if (!contacts.isEmpty()) {
//                        return contactsDataManager.getMessages(true);
//                    } else {
//                        return Observable.just(Collections.emptyList());
//                    }
//                })
//                .subscribe(messages -> {
//                    // No-op
//                }, Timber::e));
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
        bchDataManager.clearBchAccountDetails();
        DashboardPresenter.onLogout();
    }

    PayloadManager getPayloadManager() {
        return payloadManager;
    }

    // Usage commented out for now, until Contacts is back again
    @SuppressWarnings("unused")
    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);
        notificationObservable.compose(RxUtil.addObservableToCompositeDisposable(this))
                .compose(RxUtil.applySchedulersToObservable())
                .subscribe(
                        notificationPayload -> checkForMessages(),
                        Throwable::printStackTrace);
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
        return walletOptionsDataManager.getBuyWebviewWalletLink();
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
                                checkBuyAndSellEnabled();
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

    private void checkBuyAndSellEnabled() {
        buyDataManager.isSfoxAllowed()
                .compose(RxUtil.addObservableToCompositeDisposable(this))
                .subscribe(allowed -> {
                    if (allowed) getView().updateNavDrawerToBuyAndSell();
                }, Timber::e);
    }

    private void dismissAnnouncementIfOnboardingCompleted() {
        if (prefs.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)
                && prefs.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, false)) {
            prefs.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true);
        }
    }

    void decryptAndSetupMetadata(String secondPassword) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().showSecondPasswordDialog();
        } else {
            metadataManager.decryptAndSetupMetadata(secondPassword)
                    .compose(RxUtil.addCompletableToCompositeDisposable(this))
                    .subscribe(() -> appUtil.restartApp(), Throwable::printStackTrace);
        }
    }

    void setCryptoCurrency(CryptoCurrencies cryptoCurrency) {
        currencyState.setCryptoCurrency(cryptoCurrency);
    }

}
