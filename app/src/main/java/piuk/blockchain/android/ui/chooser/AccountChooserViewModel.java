package piuk.blockchain.android.ui.chooser;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.PrivateKeyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_BTC;
import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT;


public class AccountChooserViewModel extends BaseViewModel {

    private DataListener dataListener;
    private ReceiveCurrencyHelper currencyHelper;

    @Inject PrefsUtil prefsUtil;
    @Inject WalletAccountHelper walletAccountHelper;
    @Inject PrivateKeyFactory privateKeyFactory;
    @Inject PayloadManager payloadManager;
    @Inject StringUtils stringUtils;
    @Inject ContactsDataManager contactsDataManager;
    @Inject SendDataManager sendDataManager;
    @Inject MultiAddrFactory multiAddrFactory;

    private List<ItemAccount> itemAccounts = new ArrayList<>();
    private boolean isBtc;

    interface DataListener {

        void updateUi(List<ItemAccount> items);

    }

    AccountChooserViewModel(DataListener dataListener, Locale locale) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        int btcUnitType = prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC);
        MonetaryUtil monetaryUtil = new MonetaryUtil(btcUnitType);
        currencyHelper = new ReceiveCurrencyHelper(monetaryUtil, locale);

        int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        isBtc = balanceDisplayState != SHOW_FIAT;
    }

    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    @Override
    public void onViewReady() {
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .toList()
                        .doOnSuccess(contacts -> {
                            if (!contacts.isEmpty()) {
                                itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.contacts_title), null, null, null, null));
                                for (Contact contact : contacts) {
                                    itemAccounts.add(new ItemAccount(null, null, null, null, contact));
                                }
                            }
                        })
                        .flatMapObservable(contacts -> getAccountList())
                        .doOnNext(accounts -> {
                            itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.wallets), null, null, null, null));
                            itemAccounts.addAll(accounts);
                        })
                        .flatMap(accounts -> getImportedList())
                        .subscribe(
                                items -> {
                                    if (!items.isEmpty()) {
                                        itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.imported_addresses), null, null, null, null));
                                        itemAccounts.addAll(items);
                                    }
                                    dataListener.updateUi(itemAccounts);
                                }));
    }

    private Observable<List<ItemAccount>> getAccountList() {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getHdAccounts(isBtc));
        return Observable.just(result);
    }

    private Observable<List<ItemAccount>> getImportedList() {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getLegacyAddresses(isBtc));
        return Observable.just(result);
    }
}
