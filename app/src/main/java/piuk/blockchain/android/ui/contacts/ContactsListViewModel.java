package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.data.PaymentRequest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.util.PrefsUtil;

@SuppressWarnings("WeakerAccess")
public class ContactsListViewModel extends BaseViewModel {

    private static final String TAG = ContactsListViewModel.class.getSimpleName();

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PrefsUtil prefsUtil;

    interface DataListener {

        Intent getPageIntent();

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void setUiState(@ContactsListActivity.UiState int uiState);

        void showProgressDialog();

        void dismissProgressDialog();

    }

    ContactsListViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {

        // Subscribe to notification events
        subscribeToNotifications();

        dataListener.setUiState(ContactsListActivity.LOADING);
        compositeDisposable.add(
                contactsDataManager.fetchContacts()
                        .andThen(contactsDataManager.getContactList())
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> {
                                    Log.e(TAG, "onViewReady: ", throwable);
                                    dataListener.setUiState(ContactsListActivity.FAILURE);
                                }));

        // TODO: 16/11/2016 Move me to my own function. I will likely need to be called from system-wide broadcasts
        // I'm only here for testing purposes
//        compositeDisposable.add(
//                contactManager.getPaymentRequests(true)
//                        .subscribe(
//                                messages -> {
//                                    Log.d(TAG, "onViewReady: " + messages);
//                                },
//                                throwable -> {
//                                    Log.e(TAG, "onViewReady: ", throwable);
//                                    dataListener.onShowToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
//                                }));

//        Intent intent = dataListener.getPageIntent();
//        if (intent != null && intent.hasExtra(EXTRA_METADATA_URI)) {
//            MetaDataUri uriObject = MetaDataUri.decode(intent.getStringExtra(EXTRA_METADATA_URI));
//            handleIncomingUri(uriObject);
//        }
    }

    private void subscribeToNotifications() {
        FcmCallbackService.getNotificationSubject().subscribe(
                notificationPayload -> {
                    // TODO: 02/12/2016 Filter specific events that are relevant to this page
                }, throwable -> {
                    // TODO: 02/12/2016
                });
    }

    void onSendMoneyClicked(String mdid) {
        // Send payment request
        dataListener.showProgressDialog();

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(1_000_000L);
        paymentRequest.setNote("I owe you 1,000,000 satoshi for something");

//        compositeDisposable.add(
//                contactManager.sendPaymentRequest(mdid, paymentRequest)
//                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
//                        .subscribe(
//                                message -> Log.d(TAG, "onRequestMoneyClicked: " + message),
//                                throwable -> {
//                                    dataListener.onShowToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR);
//                                    Log.e(TAG, "onRequestMoneyClicked: ", throwable);
//                                }));

        // User should get back a notification with a payment request response
        // Download response from getPaymentRequestResponses(...)
    }

    void onDeleteContactClicked(String mdid) {
        dataListener.showProgressDialog();
//        compositeDisposable.add(
//                contactManager.deleteTrusted(mdid)
//                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
//                        .subscribe(success -> {
//                            if (success) {
//                                onViewReady();
//                            } else {
//                                dataListener.onShowToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR);
//                            }
//                        }, throwable -> dataListener.onShowToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR)));
    }

    // TODO: 16/11/2016

    private void handleContactListUpdate(List<Contact> contacts) {
        ArrayList<ContactsListItem> list = new ArrayList<>();

        for (Contact contact : contacts) {
            list.add(new ContactsListItem(contact.getMdid(), contact.getName(), "Trusted"));
        }

        if (!list.isEmpty()) {
            dataListener.setUiState(ContactsListActivity.CONTENT);
            dataListener.onContactsLoaded(list);
        } else {
            dataListener.setUiState(ContactsListActivity.EMPTY);
        }
    }

//    private void handleIncomingUri(MetaDataUri metaDataUri) {
//        String name = metaDataUri.getFrom();
//        dataListener.showAddContactConfirmation(name, new DialogButtonCallback() {
//            @Override
//            public void onPositiveClicked() {
//                addUser(metaDataUri);
//            }
//
//            @Override
//            public void onNegativeClicked() {
//                // Ignore
//            }
//        });
//    }
//
//    @Thunk
//    void addUser(MetaDataUri metaDataUri) {
//        String name = metaDataUri.getFrom();
//        String inviteId = metaDataUri.getInviteId();
//
//        dataListener.showProgressDialog();
//
//        compositeDisposable.add(
//                contactManager.acceptInvitation(inviteId)
//                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
//                        .flatMap(invitation -> contactManager.getContactList())
//                        .subscribe(trusted -> {
//                            handleContactListUpdate(trusted);
//                            dataListener.onShowToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
//                        }, throwable -> dataListener.onShowToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));
//    }
}
