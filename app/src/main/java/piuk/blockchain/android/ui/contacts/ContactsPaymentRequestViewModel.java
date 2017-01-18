package piuk.blockchain.android.ui.contacts;

import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactsPaymentRequestViewModel extends BaseViewModel {

    private DataListener dataListener;
    private Contact contact;
    @Inject ContactsDataManager contactsDataManager;

    interface DataListener {

        void finishPage();

        void contactLoaded(String name);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    }

    ContactsPaymentRequestViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    void loadContact(String contactId) {
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .filter(ContactsPredicates.filterById(contactId))
                        .subscribe(
                                contact -> {
                                    this.contact = contact;
                                    dataListener.contactLoaded(contact.getName());
                                },
                                throwable -> {
                                    dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                    dataListener.finishPage();
                                }));
    }

    void onNoteSet(String note) {
        // TODO: 17/01/2017 Add this to a FacilitatedTransaction object? Just store it in memory and handle later?
        // ¯\_(ツ)_/¯
    }

    void onAmountSet(long satoshis) {
        // TODO: 17/01/2017 See above
    }

    @Override
    public void onViewReady() {
        // No-op
    }
}
