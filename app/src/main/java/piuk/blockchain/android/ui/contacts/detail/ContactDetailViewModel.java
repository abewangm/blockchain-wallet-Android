package piuk.blockchain.android.ui.contacts.detail;

import android.os.Bundle;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.balance.BalanceFragment.SHOW_BTC;
import static piuk.blockchain.android.ui.contacts.list.ContactsListActivity.KEY_BUNDLE_CONTACT_ID;
import static piuk.blockchain.android.ui.send.SendViewModel.SHOW_FIAT;


public class ContactDetailViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PayloadManager payloadManager;
    @Inject PrefsUtil prefsUtil;
    private Contact contact;

    interface DataListener {

        Bundle getPageBundle();

        void updateContactName(String name);

        void finishPage();

        void showRenameDialog(String name);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        void showDeleteUserDialog();

        void disablePayments();

        void onTransactionsUpdated(List<FacilitatedTransaction> transactions);

        void startPaymentRequestActivity(PaymentRequestType paymentRequestType, String contactId);

        void showAccountChoiceDialog(List<String> accounts, String fctxId);

        void initiatePayment(String uri, String recipientId, boolean isBTC, int defaultIndex);

        void showWaitingForPaymentDialog();

        void showWaitingForAddressDialog();

        void showTransactionDetail(String txHash);
    }

    ContactDetailViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        Bundle bundle = dataListener.getPageBundle();
        if (bundle != null && bundle.getString(KEY_BUNDLE_CONTACT_ID) != null) {
            String id = bundle.getString(KEY_BUNDLE_CONTACT_ID);

            compositeDisposable.add(
                    contactsDataManager.getContactList()
                            .filter(ContactsPredicates.filterById(id))
                            .subscribe(
                                    contact -> {
                                        this.contact = contact;
                                        dataListener.updateContactName(contact.getName());

                                        List<FacilitatedTransaction> list =
                                                new ArrayList<>(contact.getFacilitatedTransaction().values());

                                        // Invert to show most recent first
                                        Collections.reverse(list);
                                        dataListener.onTransactionsUpdated(list);
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
        dataListener.startPaymentRequestActivity(PaymentRequestType.SEND, contact.getId());
    }

    void onRequestMoneyClicked() {
        dataListener.startPaymentRequestActivity(PaymentRequestType.REQUEST, contact.getId());
    }

    void onTransactionClicked(String id) {
        FacilitatedTransaction transaction = contact.getFacilitatedTransaction().get(id);

        if (transaction == null) {
            dataListener.showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR);
        } else {
            // RPR Receiver
            if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {
                if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {

                    List<String> accountNames = new ArrayList<>();
                    //noinspection Convert2streamapi
                    for (Account account : payloadManager.getPayload().getHdWallet().getAccounts()) {
                        accountNames.add(account.getLabel());
                    }
                    dataListener.showAccountChoiceDialog(accountNames, id);

                } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {

                    int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
                    boolean isBTC = balanceDisplayState != SHOW_FIAT;
                    dataListener.initiatePayment(
                            transaction.toBitcoinURI(),
                            contact.getId(),
                            isBTC,
                            payloadManager.getPayload().getHdWallet().getDefaultIndex());

                    // RPR Initiator
                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {
                    if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
                        dataListener.showWaitingForAddressDialog();

                    } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
                        dataListener.showWaitingForPaymentDialog();
                    }

                    // PR Receiver
                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {
                    if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {

                        List<String> accountNames = new ArrayList<>();
                        //noinspection Convert2streamapi
                        for (Account account : payloadManager.getPayload().getHdWallet().getAccounts()) {
                            accountNames.add(account.getLabel());
                        }
                        dataListener.showAccountChoiceDialog(accountNames, id);

                    } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {

                        int balanceDisplayState = prefsUtil.getValue(PrefsUtil.KEY_BALANCE_DISPLAY_STATE, SHOW_BTC);
                        boolean isBTC = balanceDisplayState != SHOW_FIAT;
                        dataListener.initiatePayment(
                                transaction.toBitcoinURI(),
                                contact.getId(),
                                isBTC,
                                payloadManager.getPayload().getHdWallet().getDefaultIndex());
                    }

                    // PR Initiator
                } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {
                    if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
                        dataListener.showWaitingForAddressDialog();

                    } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
                        dataListener.showWaitingForPaymentDialog();
                    }
                }
            } else if (transaction.getState().equals(FacilitatedTransaction.STATE_PAYMENT_BROADCASTED)) {
                dataListener.showTransactionDetail(transaction.getTx_hash());
            }
        }
    }

    void onAccountChosen(int accountPosition, String fctxId) {
        dataListener.showProgressDialog();
        FacilitatedTransaction transaction = contact.getFacilitatedTransaction().get(fctxId);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setIntended_amount(transaction.getIntended_amount());
        paymentRequest.setId(fctxId);

        compositeDisposable.add(
                getNextReceiveAddress(accountPosition)
                        .doOnNext(paymentRequest::setAddress)
                        .flatMapCompletable(s -> contactsDataManager.sendPaymentRequestResponse(contact.getMdid(), paymentRequest, fctxId))
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                () -> dataListener.showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK),
                                throwable -> dataListener.showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)));

    }

    private Observable<String> getNextReceiveAddress(int defaultIndex) {
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(defaultIndex));
    }

}
