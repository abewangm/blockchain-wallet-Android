package piuk.blockchain.android.ui.chooser;

import info.blockchain.wallet.contacts.data.Contact;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;


@SuppressWarnings("WeakerAccess")
public class AccountChooserViewModel extends BaseViewModel {

    private DataListener dataListener;

    @Inject PrefsUtil prefsUtil;
    @Inject WalletAccountHelper walletAccountHelper;
    @Inject StringUtils stringUtils;
    @Inject ContactsDataManager contactsDataManager;
    @Inject AccessState accessState;

    private List<ItemAccount> itemAccounts = new ArrayList<>();

    interface DataListener {

        PaymentRequestType getPaymentRequestType();

        /**
         * We can't simply call BuildConfig.CONTACTS_ENABLED in this class as it would make it
         * impossible to test, as it's reliant on the build.gradle config. Passing it here
         * allows us to change the response via mocking the DataListener.
         *
         * TODO: This should be removed once/if Contacts ships
         */
        boolean getIfContactsEnabled();

        void updateUi(List<ItemAccount> items);

        void showNoContacts();

    }

    AccountChooserViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        PaymentRequestType paymentRequestType = dataListener.getPaymentRequestType();

        if (paymentRequestType == null) {
            throw new RuntimeException("Payment request type must be passed to the Account Chooser activity");
        }

        if (paymentRequestType.equals(PaymentRequestType.SEND)) {
            if (dataListener.getIfContactsEnabled()) {
                loadReceiveAccountsAndContacts();
            } else {
                loadReceiveAccountsOnly();
            }
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
                                items -> {
                                    if (!items.isEmpty()) {
                                        dataListener.updateUi(itemAccounts);
                                    } else {
                                        dataListener.showNoContacts();
                                    }
                                },
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
                            itemAccounts.add(new ItemAccount(null, null, null, null, contact, null));
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
        result.addAll(walletAccountHelper.getHdAccounts(accessState.isBtc()));
        return Observable.just(result);
    }

    private Observable<List<ItemAccount>> getImportedList() {
        ArrayList<ItemAccount> result = new ArrayList<>();
        result.addAll(walletAccountHelper.getLegacyAddresses(accessState.isBtc()));
        return Observable.just(result);
    }

}
