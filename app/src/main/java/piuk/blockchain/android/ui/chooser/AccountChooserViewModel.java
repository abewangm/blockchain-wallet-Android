package piuk.blockchain.android.ui.chooser;

import info.blockchain.wallet.contacts.data.Contact;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_BTC;
import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT;


@SuppressWarnings("WeakerAccess")
public class AccountChooserViewModel extends BaseViewModel {

    private DataListener dataListener;

    @Inject PrefsUtil prefsUtil;
    @Inject WalletAccountHelper walletAccountHelper;
    @Inject StringUtils stringUtils;
    @Inject ContactsDataManager contactsDataManager;

    private List<ItemAccount> itemAccounts = new ArrayList<>();
    private boolean isBtc;

    interface DataListener {

        PaymentRequestType getPaymentRequestType();

        void updateUi(List<ItemAccount> items);

    }

    AccountChooserViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;

        int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
        isBtc = balanceDisplayState != SHOW_FIAT;
    }

    @Override
    public void onViewReady() {
        PaymentRequestType paymentRequestType = dataListener.getPaymentRequestType();

        if (paymentRequestType.equals(PaymentRequestType.SEND)) {
            loadReceiveAccountsAndContacts();
        } else if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {
            loadReceiveAccountsOnly();
        } else {
            loadContactsOnly();
        }
    }

    private void loadReceiveAccountsAndContacts() {
        compositeDisposable.add(
                parseContactsList()
                        .flatMapObservable(contacts -> parseAccountList())
                        .flatMap(accounts -> parseImportedList())
                        .subscribe(
                                items -> dataListener.updateUi(itemAccounts),
                                Throwable::printStackTrace));
    }

    private void loadReceiveAccountsOnly() {
        compositeDisposable.add(
                parseAccountList()
                        .flatMap(accounts -> parseImportedList())
                        .subscribe(
                                list -> dataListener.updateUi(itemAccounts),
                                Throwable::printStackTrace));
    }

    private void loadContactsOnly() {
        compositeDisposable.add(
                parseContactsList()
                        .subscribe(
                                items -> dataListener.updateUi(itemAccounts),
                                Throwable::printStackTrace));
    }

    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    private Single<List<Contact>> parseContactsList() {
        return contactsDataManager.getContactList()
                .filter(ContactsPredicates.filterByConfirmed())
                .toList()
                .doOnSuccess(contacts -> {
                    if (!contacts.isEmpty()) {
                        itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.contacts_title), null, null, null, null));
                        for (Contact contact : contacts) {
                            itemAccounts.add(new ItemAccount(null, null, null, null, contact));
                        }
                    }
                });
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ItemAccount>> parseAccountList() {
        return getAccountList()
                .doOnNext(accounts -> {
                    itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.wallets), null, null, null, null));
                    itemAccounts.addAll(accounts);
                });
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ItemAccount>> parseImportedList() {
        return getImportedList()
                .doOnNext(items -> {
                    if (!items.isEmpty()) {
                        itemAccounts.add(new ItemAccount(stringUtils.getString(R.string.imported_addresses), null, null, null, null));
                        itemAccounts.addAll(items);
                    }
                });
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
