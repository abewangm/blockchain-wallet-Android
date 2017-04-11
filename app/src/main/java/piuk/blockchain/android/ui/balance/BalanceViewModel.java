package piuk.blockchain.android.ui.balance;

import com.google.common.collect.HashBiMap;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AuthEvent;
import piuk.blockchain.android.data.contacts.ContactTransactionDateComparator;
import piuk.blockchain.android.data.contacts.ContactTransactionModel;
import piuk.blockchain.android.data.contacts.ContactsEvent;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.OnboardingDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.notifications.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ConsolidatedAccount;
import piuk.blockchain.android.ui.account.ConsolidatedAccount.Type;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class BalanceViewModel extends BaseViewModel {

    private final String TAG = getClass().getName();

    private static final long ONE_MONTH = 28 * 24 * 60 * 60 * 1000L;

    private DataListener dataListener;

    private Observable<ContactsEvent> contactsEventObservable;
    private Observable<NotificationPayload> notificationObservable;
    private Observable<List> txListObservable;
    private Observable<AuthEvent> authEventObservable;
    private List<ItemAccount> activeAccountAndAddressList;
    private HashBiMap<Object, Integer> activeAccountAndAddressBiMap;
    private List<Object> displayList;
    @Inject PrefsUtil prefsUtil;
    @Inject PayloadManager payloadManager;
    @Inject StringUtils stringUtils;
    @Inject TransactionListDataManager transactionListDataManager;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PayloadDataManager payloadDataManager;
    @Inject SettingsDataManager settingsDataManager;
    @Inject SwipeToReceiveHelper swipeToReceiveHelper;
    @Inject RxBus rxBus;
    @Inject OnboardingDataManager onboardingDataManager;
    @Inject protected ExchangeRateFactory exchangeRateFactory;
    @Inject protected PrefsUtil prefs;

    public interface DataListener {

        int getSelectedItemPosition();

        boolean isBtc();

        /**
         * We can't simply call BuildConfig.CONTACTS_ENABLED in this class as it would make it
         * impossible to test, as it's reliant on the build.gradle config. Passing it here
         * allows us to change the response via mocking the DataListener.
         *
         * TODO: This should be removed once/if Contacts ships
         */
        boolean getIfContactsEnabled();

        void onRefreshAccounts();

        void onAccountSizeChange();

        void onRefreshBalanceAndTransactions();

        void onRefreshContactList();

        void showBackupPromptDialog(boolean showNeverAgain);

        void show2FaDialog();

        void updateBalance(String balance);

        void setShowRefreshing(boolean showRefreshing);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showAccountChoiceDialog(List<String> accounts, String fctxId);

        void initiatePayment(String uri, String recipientId, String mdid, String fctxId);

        void showWaitingForPaymentDialog();

        void showWaitingForAddressDialog();

        void showSendAddressDialog(String fctxId);

        void showProgressDialog();

        void dismissProgressDialog();

        void showFctxRequiringAttention(int number);

        void showTransactionDeclineDialog(String fctxId);

        void showTransactionCancelDialog(String fctxId);

        void startBuyActivity();

        void startReceiveFragment();
    }

    public BalanceViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        activeAccountAndAddressList = new ArrayList<>();
        activeAccountAndAddressBiMap = HashBiMap.create();
        displayList = new ArrayList<>();
    }

    @Override
    public void onViewReady() {
        if (prefsUtil.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
            // 1st run of the app
            prefsUtil.setValue(PrefsUtil.KEY_FIRST_RUN, false);
        } else {
            // Check from this point forwards
            txListObservable = rxBus.register(List.class);

            if (prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0) == 3) {
                // On third visit onwards, prompt 2FA
                compositeDisposable.add(
                        txListObservable
                                .compose(RxUtil.applySchedulersToObservable())
                                .subscribe(txs -> {
                                    if (!getIfNeverPrompt2Fa()) {
                                        compositeDisposable.add(
                                                settingsDataManager.initSettings(
                                                        payloadDataManager.getWallet().getGuid(),
                                                        payloadDataManager.getWallet().getSharedKey())
                                                        .subscribe(settings -> {
                                                            if (!settings.isSmsVerified() && settings.getAuthType() == Settings.AUTH_TYPE_OFF) {
                                                                // Show dialog for 2FA, store date of dialog launch
                                                                if (getTimeOfLastSecurityPrompt() == 0L
                                                                        || (System.currentTimeMillis() - getTimeOfLastSecurityPrompt()) >= ONE_MONTH) {
                                                                    dataListener.show2FaDialog();
                                                                    storeTimeOfLastSecurityPrompt();
                                                                }
                                                            }
                                                        }, Throwable::printStackTrace));
                                    }
                                }, Throwable::printStackTrace));
            } else {
                // From second visit onwards, prompt backup if not already
                compositeDisposable.add(
                        txListObservable
                                .compose(RxUtil.applySchedulersToObservable())
                                .subscribe(txs -> {
                                    if (hasTransactions() && !isBackedUp() && !getIfNeverPromptBackup()) {
                                        // Show dialog and store date of dialog launch
                                        if (getTimeOfLastSecurityPrompt() == 0) {
                                            dataListener.showBackupPromptDialog(false);
                                            storeTimeOfLastSecurityPrompt();
                                        } else if ((System.currentTimeMillis() - getTimeOfLastSecurityPrompt()) >= ONE_MONTH) {
                                            dataListener.showBackupPromptDialog(true);
                                            storeTimeOfLastSecurityPrompt();
                                        }
                                    }
                                }, Throwable::printStackTrace));
            }
        }

        contactsEventObservable = rxBus.register(ContactsEvent.class);
        contactsEventObservable.subscribe(contactsEvent -> refreshFacilitatedTransactions());

        authEventObservable = rxBus.register(AuthEvent.class);
        authEventObservable.subscribe(authEvent -> {
            displayList.clear();
            transactionListDataManager.clearTransactionList();
            contactsDataManager.resetContacts();
        });

        notificationObservable = rxBus.register(NotificationPayload.class);
        notificationObservable
                .subscribe(notificationPayload -> {
                    if (notificationPayload.getType() != null
                            && notificationPayload.getType().equals(NotificationPayload.NotificationType.PAYMENT)) {
                        refreshFacilitatedTransactions();
                    }
                });
    }

    boolean areLauncherShortcutsEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true);
    }

    public PayloadDataManager getPayloadDataManager() {
        return payloadDataManager;
    }

    void storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        compositeDisposable.add(
                Completable.fromCallable(() -> {
                    swipeToReceiveHelper.updateAndStoreAddresses();
                    return Void.TYPE;
                }).subscribeOn(Schedulers.computation())
                        .subscribe(() -> {
                            // No-op
                        }, Throwable::printStackTrace));
    }

    @SuppressWarnings("Convert2streamapi")
    void updateAccountList() {
        //activeAccountAndAddressList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        activeAccountAndAddressList.clear();
        activeAccountAndAddressBiMap.clear();

        int spinnerIndex = 0;

        //All accounts/addresses
        List<Account> allAccounts;
        List<LegacyAddress> allLegacyAddresses = payloadManager.getPayload().getLegacyAddressList();

        //Only active accounts/addresses (exclude archived)
        List<Account> activeAccounts = new ArrayList<>();
        allAccounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();//V3

        for (Account item : allAccounts) {
            if (!item.isArchived()) {
                activeAccounts.add(item);
            }
        }

        List<LegacyAddress> activeLegacyAddresses = new ArrayList<>();
        for (LegacyAddress item : allLegacyAddresses) {
            if (item.getTag() != LegacyAddress.ARCHIVED_ADDRESS) {
                activeLegacyAddresses.add(item);
            }
        }

        //"All" - total balance
        if (activeAccounts.size() > 1 || !activeLegacyAddresses.isEmpty()) {
            //Only V3 will display "All"
            ConsolidatedAccount all = new ConsolidatedAccount();
            all.setLabel(stringUtils.getString(R.string.all_accounts));
            all.setType(Type.ALL_ACCOUNTS);

            BigInteger bal = payloadManager.getWalletBalance();
            String balance = getBalanceString(true, bal.longValue());
            activeAccountAndAddressList.add(new ItemAccount(
                    all.getLabel(),
                    balance,
                    null,
                    bal.longValue(),
                    null));
            activeAccountAndAddressBiMap.put(all, spinnerIndex);
            spinnerIndex++;
        }

        //Add accounts to map
        int accountIndex = 0;
        for (Account item : activeAccounts) {

            //Give unlabeled account a label
            if (item.getLabel().trim().isEmpty()) item.setLabel("Account: " + accountIndex);

            BigInteger bal = payloadDataManager.getAddressBalance(item.getXpub());
            String balanceString = getBalanceString(true, bal.longValue());

            activeAccountAndAddressList.add(new ItemAccount(
                    item.getLabel(),
                    balanceString,
                    null,
                    bal.longValue(),
                    null));
            activeAccountAndAddressBiMap.put(item, spinnerIndex);
            spinnerIndex++;
            accountIndex++;
        }

        //Add "Imported Addresses" or "Total Funds" to map
        if (!activeLegacyAddresses.isEmpty()) {
            //Only V3 - Consolidate and add Legacy addresses to "Imported Addresses" at bottom of accounts spinner

            ConsolidatedAccount importedAddresses = new ConsolidatedAccount();
            importedAddresses.setLabel(stringUtils.getString(R.string.imported_addresses));
            importedAddresses.setType(Type.ALL_IMPORTED_ADDRESSES);

            BigInteger bal = payloadManager.getImportedAddressesBalance();
            String balance = getBalanceString(true, bal.longValue());

            activeAccountAndAddressList.add(new ItemAccount(
                    importedAddresses.getLabel(),
                    balance,
                    null,
                    bal.longValue(),
                    null));
            activeAccountAndAddressBiMap.put(importedAddresses, spinnerIndex);
        }

        //If we have multiple accounts/addresses we will show dropdown in toolbar, otherwise we will only display a static text
        if (dataListener != null) {
            dataListener.onRefreshAccounts();
        }
    }

    List<Object> getTransactionList() {
        return displayList;
    }

    HashMap<String, String> getContactsTransactionMap() {
        return contactsDataManager.getContactsTransactionMap();
    }

    HashMap<String, String> getNotesTransactionMap() {
        return contactsDataManager.getNotesTransactionMap();
    }

    void onTransactionListRefreshed() {
        compositeDisposable.add(
                payloadDataManager.updateAllTransactions()
                        .doOnSubscribe(disposable -> dataListener.setShowRefreshing(true))
                        .doAfterTerminate(() -> dataListener.setShowRefreshing(false))
                        .subscribe(() -> {
                            updateAccountList();
                            refreshFacilitatedTransactions();
                            updateBalanceAndTransactionList(dataListener.getSelectedItemPosition(), dataListener.isBtc(), true);
                        }, throwable -> {
                            // No-op
                        }));
    }

    void updateBalanceAndTransactionList(int accountSpinnerPosition, boolean isBTC, boolean fetchTransactions) {
        // The current selected item in dropdown (Account or Legacy Address)
        Object object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);

        //If current selected item gets edited by another platform object might become null
        if (object == null && dataListener != null) {
            dataListener.onAccountSizeChange();
            object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);
        }

        //Update balance
        final Object finalObject = object;
        compositeDisposable.add(
                payloadDataManager.updateAllBalances()
                        .subscribe(() -> {
                            long btcBalance = transactionListDataManager.getBtcBalance(finalObject);
                            String balanceTotal = getBalanceString(isBTC, btcBalance);

                            if (dataListener != null) {
                                dataListener.updateBalance(balanceTotal);
                            }
                        }));

        if (fetchTransactions) {
            //Update transactions
            compositeDisposable.add(
                    transactionListDataManager.fetchTransactions(object, 50, 0)
                            .doAfterTerminate(() -> {
                                if (dataListener != null) dataListener.setShowRefreshing(false);
                            })
                            .subscribe(
                                    this::insertTransactionsAndDisplay,
                                    throwable -> Log.e(TAG, "updateBalanceAndTransactionList: ", throwable)));
        }
    }

    @SuppressWarnings("Java8CollectionRemoveIf")
    private void insertTransactionsAndDisplay(List<TransactionSummary> txList) throws IOException, ApiException {
        // Remove current transactions but keep headers and pending transactions
        Iterator iterator = displayList.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof TransactionSummary) {
                iterator.remove();
            }
        }

        displayList.addAll(txList);

        if (dataListener != null) {
            dataListener.onRefreshBalanceAndTransactions();
        }
    }

    void refreshFacilitatedTransactions() {
        if (dataListener.getIfContactsEnabled()) {
            compositeDisposable.add(
                    contactsDataManager.fetchContacts()
                            .andThen(contactsDataManager.getContactsWithUnreadPaymentRequests())
                            .toList()
                            .doOnSuccess(contacts -> dataListener.onRefreshContactList())
                            .flatMapObservable(contacts -> contactsDataManager.refreshFacilitatedTransactions())
                            .toList()
                            .subscribe(
                                    this::handlePendingTransactions,
                                    Throwable::printStackTrace));
        }
    }

    /**
     * Cached transactions. To be called sometime in the future
     */
    void getFacilitatedTransactions() {
        compositeDisposable.add(
                contactsDataManager.getFacilitatedTransactions()
                        .toList()
                        .doOnSuccess(contacts -> dataListener.onRefreshContactList())
                        .subscribe(
                                this::handlePendingTransactions,
                                Throwable::printStackTrace));
    }

    void onPendingTransactionClicked(String fctxId) {
        compositeDisposable.add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .subscribe(contact -> {
                            FacilitatedTransaction transaction = contact.getFacilitatedTransactions().get(fctxId);

                            if (transaction == null) {
                                dataListener.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR);
                            } else {

                                // Payment request sent, waiting for address from recipient
                                if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                                        && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {

                                    dataListener.showWaitingForAddressDialog();

                                    // Payment request sent, waiting for payment
                                } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                                        && transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {

                                    dataListener.showWaitingForPaymentDialog();

                                    // Received payment request, need to send address to sender
                                } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                                        && transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {

                                    List<String> accountNames = new ArrayList<>();
                                    //noinspection Convert2streamapi
                                    for (Account account : payloadManager.getPayload().getHdWallets().get(0).getAccounts()) {
                                        if (!account.isArchived()) {
                                            accountNames.add(account.getLabel());
                                        }
                                    }

                                    if (accountNames.size() == 1) {
                                        // Only one account, ask if you want to send an address
                                        dataListener.showSendAddressDialog(fctxId);
                                    } else {
                                        // Show dialog allowing user to select which account they want to use
                                        dataListener.showAccountChoiceDialog(accountNames, fctxId);
                                    }

                                    // Waiting for payment
                                } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                                        && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {

                                    dataListener.initiatePayment(
                                            transaction.toBitcoinURI(),
                                            contact.getId(),
                                            contact.getMdid(),
                                            transaction.getId());
                                }
                            }
                        }, throwable -> dataListener.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)));
    }

    void onPendingTransactionLongClicked(String fctxId) {
        compositeDisposable.add(
                contactsDataManager.getFacilitatedTransactions()
                        .filter(contactTransactionModel -> contactTransactionModel.getFacilitatedTransaction().getId().equals(fctxId))
                        .subscribe(contactTransactionModel -> {
                            FacilitatedTransaction fctx = contactTransactionModel.getFacilitatedTransaction();

                            if (fctx.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
                                if (fctx.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                                    dataListener.showTransactionDeclineDialog(fctxId);
                                } else if (fctx.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {
                                    dataListener.showTransactionCancelDialog(fctxId);
                                }

                            } else if (fctx.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
                                if (fctx.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                                    dataListener.showTransactionDeclineDialog(fctxId);
                                } else if (fctx.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {
                                    dataListener.showTransactionCancelDialog(fctxId);
                                }
                            }
                        }, throwable -> {
                            // No-op
                        }));
    }

    void confirmDeclineTransaction(String fctxId) {
        compositeDisposable.add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .flatMapCompletable(contact -> contactsDataManager.sendPaymentDeclinedResponse(contact.getMdid(), fctxId))
                        .doOnError(throwable -> contactsDataManager.fetchContacts())
                        .doAfterTerminate(this::refreshFacilitatedTransactions)
                        .subscribe(
                                () -> dataListener.showToast(R.string.contacts_pending_transaction_decline_success, ToastCustom.TYPE_OK),
                                throwable -> dataListener.showToast(R.string.contacts_pending_transaction_decline_failure, ToastCustom.TYPE_ERROR)));
    }

    void confirmCancelTransaction(String fctxId) {
        compositeDisposable.add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .flatMapCompletable(contact -> contactsDataManager.sendPaymentCancelledResponse(contact.getMdid(), fctxId))
                        .doOnError(throwable -> contactsDataManager.fetchContacts())
                        .doAfterTerminate(this::refreshFacilitatedTransactions)
                        .subscribe(
                                () -> dataListener.showToast(R.string.contacts_pending_transaction_cancel_success, ToastCustom.TYPE_OK),
                                throwable -> dataListener.showToast(R.string.contacts_pending_transaction_cancel_failure, ToastCustom.TYPE_ERROR)));
    }

    void onAccountChosen(int accountPosition, String fctxId) {
        dataListener.showProgressDialog();
        compositeDisposable.add(
                contactsDataManager.getContactFromFctxId(fctxId)
                        .subscribe(contact -> {
                            FacilitatedTransaction transaction = contact.getFacilitatedTransactions().get(fctxId);

                            PaymentRequest paymentRequest = new PaymentRequest();
                            paymentRequest.setIntendedAmount(transaction.getIntendedAmount());
                            paymentRequest.setId(fctxId);

                            compositeDisposable.add(
                                    payloadDataManager.getNextReceiveAddressAndReserve(getCorrectedAccountIndex(accountPosition), "Payment request " + transaction.getId())
                                            .doOnNext(paymentRequest::setAddress)
                                            .flatMapCompletable(s -> contactsDataManager.sendPaymentRequestResponse(contact.getMdid(), paymentRequest, fctxId))
                                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                                            .subscribe(
                                                    () -> {
                                                        dataListener.showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK);
                                                        refreshFacilitatedTransactions();
                                                    },
                                                    throwable -> dataListener.showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)));
                        }, throwable -> dataListener.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)));
    }

    @NonNull
    private String getBalanceString(boolean isBTC, long btcBalance) {
        double fiatBalance;
        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double lastPrice = ExchangeRateFactory.getInstance().getLastPrice(strFiat);
        fiatBalance = lastPrice * (btcBalance / 1e8);

        return isBTC ? getMonetaryUtil().getDisplayAmountWithFormatting(btcBalance) + " " + getDisplayUnits()
                : getMonetaryUtil().getFiatFormat(strFiat).format(fiatBalance) + " " + strFiat;
    }

    public String getDisplayUnits() {
        return (String) getMonetaryUtil().getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    /**
     * Saves value of true to prevent users from seeing the backup prompt again
     */
    public void neverPromptBackup() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, true);
    }

    /**
     * Saves value of true to prevent users from seeing the 2FA prompt again
     */
    public void neverPrompt2Fa() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, true);
    }

    public List<ItemAccount> getActiveAccountAndAddressList() {
        return activeAccountAndAddressList;
    }

    public StringUtils getStringUtils() {
        return stringUtils;
    }

    public PayloadManager getPayloadManager() {
        return payloadManager;
    }

    public MonetaryUtil getMonetaryUtil() {
        return new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public PrefsUtil getPrefsUtil() {
        return prefsUtil;
    }

    private boolean isBackedUp() {
        return payloadManager.getPayload() != null
                && payloadManager.getPayload().getHdWallets() != null
                && payloadManager.getPayload().getHdWallets().get(0).isMnemonicVerified();
    }

    private boolean hasTransactions() {
        return !transactionListDataManager.getTransactionList().isEmpty();
    }

    private long getTimeOfLastSecurityPrompt() {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, 0L);
    }

    private void storeTimeOfLastSecurityPrompt() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, System.currentTimeMillis());
    }

    private boolean getIfNeverPromptBackup() {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, false);
    }

    private boolean getIfNeverPrompt2Fa() {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, false);
    }

    private int getCorrectedAccountIndex(int accountIndex) {
        // Filter accounts by active
        List<Account> activeAccounts = new ArrayList<>();
        List<Account> accounts = payloadManager.getPayload().getHdWallets().get(0).getAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            if (!account.isArchived()) {
                activeAccounts.add(account);
            }
        }

        // Find corrected position
        return payloadManager.getPayload().getHdWallets().get(0).getAccounts().indexOf(activeAccounts.get(accountIndex));
    }

    private int getNumberOfFctxRequiringAttention(List<ContactTransactionModel> facilitatedTransactions) {
        int value = 0;
        for (ContactTransactionModel transactionModel : facilitatedTransactions) {
            FacilitatedTransaction transaction = transactionModel.getFacilitatedTransaction();
            if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                value++;
            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
                    && transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                value++;
            }
        }
        return value;
    }

    @SuppressWarnings("Java8CollectionRemoveIf")
    private void handlePendingTransactions(List<ContactTransactionModel> transactions) {
        // Remove previous Pending Transactions
        Iterator iterator = displayList.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (!(element instanceof TransactionSummary)) {
                iterator.remove();
            }
        }

        dataListener.showFctxRequiringAttention(getNumberOfFctxRequiringAttention(transactions));

        if (!transactions.isEmpty()) {
            //noinspection Java8ListSort
            Collections.sort(transactions, new ContactTransactionDateComparator());
            Collections.reverse(transactions);
            displayList.add(0, stringUtils.getString(R.string.contacts_pending_transaction));
            displayList.addAll(1, transactions);
            displayList.add(transactions.size() + 1, stringUtils.getString(R.string.contacts_transaction_history));
            dataListener.onRefreshBalanceAndTransactions();
        } else {
            dataListener.onRefreshBalanceAndTransactions();
        }
    }

    @Override
    public void destroy() {
        rxBus.unregister(ContactsEvent.class, contactsEventObservable);
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        rxBus.unregister(List.class, txListObservable);
        rxBus.unregister(AuthEvent.class, authEventObservable);
        super.destroy();
    }

    public List<OnboardingPagerContent> getOnboardingPages() {

        List<OnboardingPagerContent> pages = new ArrayList<>();

        if (onboardingDataManager.isSepa() && BuildConfig.BUY_BITCOIN_ENABLED) {
            pages.add(new OnboardingPagerContent(stringUtils.getString(R.string.onboarding_current_price),
                    getFormattedPriceString(),
                    stringUtils.getString(R.string.onboarding_buy_content),
                    stringUtils.getString(R.string.onboarding_buy_bitcoin),
                    MainActivity.ACTION_BUY,
                    R.color.primary_blue_accent,
                    R.drawable.vector_buy_offset));
        }

        //Receive bitcoin
        pages.add(new OnboardingPagerContent(stringUtils.getString(R.string.onboarding_receive_bitcoin),
                "",
                stringUtils.getString(R.string.onboarding_receive_content),
                stringUtils.getString(R.string.receive_bitcoin),
                MainActivity.ACTION_RECEIVE,
                R.color.secondary_teal_medium,
                R.drawable.vector_receive_offset));

        //QR Codes
        pages.add(new OnboardingPagerContent(stringUtils.getString(R.string.onboarding_qr_codes),
                "",
                stringUtils.getString(R.string.onboarding_qr_codes_content),
                stringUtils.getString(R.string.onboarding_scan_address),
                MainActivity.ACTION_SEND,
                R.color.primary_navy_medium,
                R.drawable.vector_qr_offset));
        return pages;
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

    public boolean isOnboardingComplete() {
        return prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false);
    }

    public void setOnboardingComplete(boolean competed) {
        prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, competed);
    }

    public void getBitcoinClicked() {
        if (onboardingDataManager.isSepa() && BuildConfig.BUY_BITCOIN_ENABLED) {
            dataListener.startBuyActivity();
        } else {
            dataListener.startReceiveFragment();
        }
    }
}
