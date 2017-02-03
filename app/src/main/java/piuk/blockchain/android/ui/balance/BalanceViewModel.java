package piuk.blockchain.android.ui.balance;

import com.google.common.collect.HashBiMap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;

import info.blockchain.api.Settings;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.ImportedAccount;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.transaction.Tx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class BalanceViewModel extends BaseViewModel {

    private static final long ONE_MONTH = 28 * 24 * 60 * 60 * 1000L;

    private DataListener dataListener;

    private List<ItemAccount> activeAccountAndAddressList;
    private HashBiMap<Object, Integer> activeAccountAndAddressBiMap;
    private List<Tx> transactionList;
    @Inject PrefsUtil prefsUtil;
    @Inject PayloadManager payloadManager;
    @Inject StringUtils stringUtils;
    @Inject TransactionListDataManager transactionListDataManager;
    @Inject ContactsDataManager contactsDataManager;

    public interface DataListener {

        int getSelectedItemPosition();

        boolean isBtc();

        void onRefreshAccounts();

        void onAccountSizeChange();

        void onRefreshBalanceAndTransactions();

        void showBackupPromptDialog(boolean showNeverAgain);

        void show2FaDialog();

        void updateBalance(String balance);

        void setShowRefreshing(boolean showRefreshing);
    }

    public BalanceViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        activeAccountAndAddressList = new ArrayList<>();
        activeAccountAndAddressBiMap = HashBiMap.create();
        transactionList = new ArrayList<>();
    }

    @Override
    public void onViewReady() {
        if (prefsUtil.getValue(PrefsUtil.KEY_FIRST_RUN, true)) {
            // 1st run of the app
            prefsUtil.setValue(PrefsUtil.KEY_FIRST_RUN, false);
        } else {
            // Check from this point forwards
            compositeDisposable.add(
                    transactionListDataManager.getListUpdateSubject()
                            .compose(RxUtil.applySchedulersToObservable())
                            .subscribe(txs -> {
                                if (hasTransactions()) {
                                    if (!isBackedUp() && !getIfNeverPromptBackup()) {
                                        // Show dialog and store date of dialog launch
                                        if (getTimeOfLastSecurityPrompt() == 0) {
                                            dataListener.showBackupPromptDialog(false);
                                            storeTimeOfLastSecurityPrompt();
                                        } else if ((System.currentTimeMillis() - getTimeOfLastSecurityPrompt()) >= ONE_MONTH) {
                                            dataListener.showBackupPromptDialog(true);
                                            storeTimeOfLastSecurityPrompt();
                                        }
                                    } else if (isBackedUp() && !getIfNeverPrompt2Fa()) {
                                        compositeDisposable.add(
                                                getSettingsApi()
                                                        .compose(RxUtil.applySchedulersToObservable())
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
                                }

                            }, Throwable::printStackTrace));
        }
    }

    @SuppressWarnings("Convert2streamapi")
    public void updateAccountList() {
        //activeAccountAndAddressList is linked to Adapter - do not reconstruct or loose reference otherwise notifyDataSetChanged won't work
        activeAccountAndAddressList.clear();
        activeAccountAndAddressBiMap.clear();

        int spinnerIndex = 0;

        //All accounts/addresses
        List<Account> allAccounts;
        List<LegacyAddress> allLegacyAddresses = payloadManager.getPayload().getLegacyAddressList();

        //Only active accounts/addresses (exclude archived)
        List<Account> activeAccounts = new ArrayList<>();
        if (payloadManager.getPayload().isUpgraded()) {

            allAccounts = payloadManager.getPayload().getHdWallet().getAccounts();//V3

            for (Account item : allAccounts) {
                if (!item.isArchived()) {
                    activeAccounts.add(item);
                }
            }
        }
        List<LegacyAddress> activeLegacyAddresses = new ArrayList<>();
        for (LegacyAddress item : allLegacyAddresses) {
            if (item.getTag() != LegacyAddress.ARCHIVED_ADDRESS) {
                activeLegacyAddresses.add(item);
            }
        }

        //"All" - total balance
        if (activeAccounts.size() > 1 || activeLegacyAddresses.size() > 0) {
            if (payloadManager.getPayload().isUpgraded()) {

                //Only V3 will display "All"
                Account all = new Account();
                all.setLabel(stringUtils.getString(R.string.all_accounts));
                all.setRealIdx(TransactionListDataManager.INDEX_ALL_REAL);
                String balance = getBalanceString(true, transactionListDataManager.getBtcBalance(all));
                activeAccountAndAddressList.add(new ItemAccount(
                        all.getLabel(),
                        balance,
                        null,
                        Math.round(transactionListDataManager.getBtcBalance(all)),
                        null));
                activeAccountAndAddressBiMap.put(all, spinnerIndex);
                spinnerIndex++;

            } else if (activeLegacyAddresses.size() > 1) {

                //V2 "All" at top of accounts spinner if wallet contains multiple legacy addresses
                ImportedAccount iAccount = new ImportedAccount(stringUtils.getString(R.string.total_funds),
                        payloadManager.getPayload().getLegacyAddressList(),
                        MultiAddrFactory.getInstance().getLegacyBalance());
                iAccount.setRealIdx(TransactionListDataManager.INDEX_IMPORTED_ADDRESSES);
                String balance = getBalanceString(true, transactionListDataManager.getBtcBalance(iAccount));
                activeAccountAndAddressList.add(new ItemAccount(
                        iAccount.getLabel(),
                        balance,
                        null,
                        Math.round(transactionListDataManager.getBtcBalance(iAccount)),
                        null));
                activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
                spinnerIndex++;
            }
        }

        //Add accounts to map
        int accountIndex = 0;
        for (Account item : activeAccounts) {

            //Give unlabeled account a label
            if (item.getLabel().trim().length() == 0) item.setLabel("Account: " + accountIndex);
            String balance = getBalanceString(true, transactionListDataManager.getBtcBalance(item));
            activeAccountAndAddressList.add(new ItemAccount(
                    item.getLabel(),
                    balance,
                    null,
                    Math.round(transactionListDataManager.getBtcBalance(item)),
                    null));
            activeAccountAndAddressBiMap.put(item, spinnerIndex);
            spinnerIndex++;
            accountIndex++;
        }

        //Add "Imported Addresses" or "Total Funds" to map
        if (payloadManager.getPayload().isUpgraded() && activeLegacyAddresses.size() > 0) {
            //Only V3 - Consolidate and add Legacy addresses to "Imported Addresses" at bottom of accounts spinner
            ImportedAccount iAccount = new ImportedAccount(stringUtils.getString(R.string.imported_addresses),
                    payloadManager.getPayload().getLegacyAddressList(),
                    MultiAddrFactory.getInstance().getLegacyBalance());
            iAccount.setRealIdx(TransactionListDataManager.INDEX_IMPORTED_ADDRESSES);
            String balance = getBalanceString(true, transactionListDataManager.getBtcBalance(iAccount));
            activeAccountAndAddressList.add(new ItemAccount(
                    iAccount.getLabel(),
                    balance,
                    null,
                    Math.round(transactionListDataManager.getBtcBalance(iAccount)),
                    null));
            activeAccountAndAddressBiMap.put(iAccount, spinnerIndex);
            spinnerIndex++;

        } else {
            for (LegacyAddress legacyAddress : activeLegacyAddresses) {
                //If address has no label, we'll display address
                String labelOrAddress = legacyAddress.getLabel() == null ||
                        legacyAddress.getLabel().trim().length() == 0 ?
                        legacyAddress.getAddress() : legacyAddress.getLabel();

                //Prefix "watch-only"
                if (legacyAddress.isWatchOnly()) {
                    labelOrAddress = stringUtils.getString(R.string.watch_only_label) + " " + labelOrAddress;
                }

                String balance = getBalanceString(true, transactionListDataManager.getBtcBalance(legacyAddress));
                activeAccountAndAddressList.add(new ItemAccount(
                        labelOrAddress,
                        balance,
                        null,
                        Math.round(transactionListDataManager.getBtcBalance(legacyAddress)),
                        null));
                activeAccountAndAddressBiMap.put(legacyAddress, spinnerIndex);
                spinnerIndex++;
            }
        }

        //If we have multiple accounts/addresses we will show dropdown in toolbar, otherwise we will only display a static text
        if (dataListener != null) dataListener.onRefreshAccounts();
    }

    public List<Object> getTransactionList() {
        return new ArrayList<>(transactionList);
    }

    public void onTransactionListRefreshed() {
        dataListener.setShowRefreshing(true);
        compositeDisposable.add(
                updateBalancesAndTransactions()
                        .doAfterTerminate(() -> dataListener.setShowRefreshing(false))
                        .subscribe(() -> {
                            updateAccountList();
                            updateBalanceAndTransactionList(null, dataListener.getSelectedItemPosition(), dataListener.isBtc());
                        }, throwable -> {
                            // No-op
                        }));
    }

    public void updateBalanceAndTransactionList(Intent intent, int accountSpinnerPosition, boolean isBTC) {
        Object object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);//the current selected item in dropdown (Account or Legacy Address)

        //If current selected item gets edited by another platform object might become null
        if (object == null && dataListener != null) {
            dataListener.onAccountSizeChange();
            object = activeAccountAndAddressBiMap.inverse().get(accountSpinnerPosition);
        }

        transactionListDataManager.clearTransactionList();
        transactionListDataManager.generateTransactionList(object);
        transactionList = transactionListDataManager.getTransactionList();
        double btcBalance = transactionListDataManager.getBtcBalance(object);

        // Returning from SendFragment the following will happen
        // After sending btc we create a "placeholder" tx until websocket handler refreshes list
        if (intent != null && intent.getExtras() != null) {
            long amount = intent.getLongExtra("queued_bamount", 0);
            String strNote = intent.getStringExtra("queued_strNote");
            String direction = intent.getStringExtra("queued_direction");
            long time = intent.getLongExtra("queued_time", System.currentTimeMillis() / 1000);

            @SuppressLint("UseSparseArrays")
            Tx tx = new Tx("", strNote, direction, amount, time, new HashMap<>());

            transactionList = transactionListDataManager.insertTransactionIntoListAndReturnSorted(tx);
        } else if (transactionList.size() > 0) {
            if (transactionList.get(0).getHash().isEmpty()) transactionList.remove(0);
        }

        String balanceTotal = getBalanceString(isBTC, btcBalance);

        if (dataListener != null) {
            dataListener.updateBalance(balanceTotal);
            dataListener.onRefreshBalanceAndTransactions();
        }
    }

    private void updateFacilitatedTransactions() {
        compositeDisposable.add(
                contactsDataManager.fetchContacts()
                        .andThen(contactsDataManager.getUnfulfilledFacilitatedTransactions())
                        .toList()
                        .subscribe(
                                transactions -> {
                                    // TODO: 03/02/2017
                                }, throwable -> {
                                    // TODO: 03/02/2017
                                }));
    }

    @NonNull
    private String getBalanceString(boolean isBTC, double btcBalance) {
        double fiatBalance;
        String strFiat = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double lastPrice = ExchangeRateFactory.getInstance().getLastPrice(strFiat);
        fiatBalance = lastPrice * (btcBalance / 1e8);

        String balanceTotal;
        balanceTotal =
                isBTC ? getMonetaryUtil().getDisplayAmountWithFormatting(btcBalance) + " " + getDisplayUnits()
                        : getMonetaryUtil().getFiatFormat(strFiat).format(fiatBalance) + " " + strFiat;

        return balanceTotal;
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
                && payloadManager.getPayload().getHdWallet() != null
                && payloadManager.getPayload().getHdWallet().isMnemonicVerified();
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

    private Observable<Settings> getSettingsApi() {
        return Observable.fromCallable(() -> new Settings(payloadManager.getPayload().getGuid(), payloadManager.getPayload().getSharedKey()));
    }

    private Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.updateBalancesAndTransactions();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

}
