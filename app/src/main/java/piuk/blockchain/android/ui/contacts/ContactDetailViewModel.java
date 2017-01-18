package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

import static piuk.blockchain.android.ui.contacts.ContactsListActivity.KEY_BUNDLE_ID;


public class ContactDetailViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject ContactsDataManager contactsDataManager;
    private Contact contact;

    interface DataListener {

        Intent getPageIntent();

        void updateContactName(String name);

        void finishPage();

        void showRenameDialog(String name);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        void showDeleteUserDialog();

        void disablePayments();

        void onTransactionsUpdated(List<FacilitatedTransaction> transactions);

        void startPaymentRequestActivity(ContactPaymentRequestActivity.PaymentRequestType paymentRequestType, String contactId);

    }

    ContactDetailViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        Intent pageIntent = dataListener.getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(KEY_BUNDLE_ID)) {
            String id = pageIntent.getStringExtra(KEY_BUNDLE_ID);

            compositeDisposable.add(
                    contactsDataManager.getContactList()
                            .filter(ContactsPredicates.filterById(id))
                            .subscribe(
                                    contact -> {
                                        this.contact = contact;
                                        dataListener.updateContactName(contact.getName());
                                        dataListener.onTransactionsUpdated(contact.getFacilitatedTransaction() != null
                                                ? new ArrayList<>(contact.getFacilitatedTransaction().values()) : Collections.emptyList());
                                        if (contact.getMdid() == null || contact.getMdid().isEmpty()) {
                                            dataListener.disablePayments();
                                        }
                                    }, throwable -> showErrorAndQuitPage()));
        } else {
            showErrorAndQuitPage();
        }
    }

    private void showErrorAndQuitPage() {
        dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
        dataListener.finishPage();
    }

    void onDeleteContactClicked() {
        dataListener.showDeleteUserDialog();
    }

    void onDeleteContactConfirmed() {
        dataListener.showProgressDialog();
        compositeDisposable.add(
                contactsDataManager.removeContact(contact)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(() -> {
                            // Quit page, show toast
                            dataListener.showToast(R.string.contacts_delete_contact_success, ToastCustom.TYPE_GENERAL);
                            dataListener.finishPage();
                        }, throwable -> dataListener.showToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR)));
    }

    void onRenameContactClicked() {
        dataListener.showRenameDialog(contact.getName());
    }

    void onContactRenamed(String name) {
        //noinspection StatementWithEmptyBody
        if (name.equals(contact.getName())) {
            // No problem here
        } else if (name.isEmpty()) {
            dataListener.showToast(R.string.contacts_rename_invalid_name, ToastCustom.TYPE_ERROR);
        } else {
            dataListener.showProgressDialog();

            contact.setName(name);
            compositeDisposable.add(
                    contactsDataManager.saveContacts()
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                            .subscribe(
                                    () -> {
                                        dataListener.updateContactName(name);
                                        dataListener.showToast(R.string.contacts_rename_success, ToastCustom.TYPE_GENERAL);
                                    },
                                    throwable -> dataListener.showToast(R.string.contacts_rename_failed, ToastCustom.TYPE_ERROR)));
        }
    }

    void onSendMoneyClicked() {
        dataListener.startPaymentRequestActivity(
                ContactPaymentRequestActivity.PaymentRequestType.SEND, contact.getId());
    }

    void onRequestMoneyClicked() {
        dataListener.startPaymentRequestActivity(
                ContactPaymentRequestActivity.PaymentRequestType.REQUEST, contact.getId());
    }

}
