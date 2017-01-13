package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import info.blockchain.wallet.contacts.data.Contact;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.PrefsUtil;

import static piuk.blockchain.android.ui.contacts.ContactsListActivity.EXTRA_METADATA_URI;

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

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

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
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> dataListener.setUiState(ContactsListActivity.FAILURE)));

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
//                                    dataListener.showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
//                                }));

//        Intent intent = dataListener.getPageIntent();
//        if (intent != null && intent.hasExtra(EXTRA_METADATA_URI)) {
//            MetaDataUri uriObject = MetaDataUri.decode(intent.getStringExtra(EXTRA_METADATA_URI));
//            handleIncomingUri(uriObject);
//        }

        Intent intent = dataListener.getPageIntent();
        if (intent != null && intent.hasExtra(EXTRA_METADATA_URI)) {
            String data = intent.getStringExtra(EXTRA_METADATA_URI);
            handleLink(data);
        }
    }

    /**
     * Get the latest version stored on disk
     */
    void requestUpdatedList() {
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> dataListener.setUiState(ContactsListActivity.FAILURE)));
    }

    private void subscribeToNotifications() {
        FcmCallbackService.getNotificationSubject().subscribe(
                notificationPayload -> {
                    Log.d(TAG, "subscribeToNotifications: ");
                    // TODO: 02/12/2016 Filter specific events that are relevant to this page
                }, throwable -> {
                    Log.e(TAG, "subscribeToNotifications: ", throwable);
                });
    }

    private void handleContactListUpdate(List<Contact> contacts) {
        ArrayList<ContactsListItem> list = new ArrayList<>();

        for (Contact contact : contacts) {
            list.add(new ContactsListItem(contact.getId(), contact.getName(), "Trusted"));
        }

        if (!list.isEmpty()) {
            dataListener.setUiState(ContactsListActivity.CONTENT);
            dataListener.onContactsLoaded(list);
        } else {
            dataListener.onContactsLoaded(new ArrayList<>());
            dataListener.setUiState(ContactsListActivity.EMPTY);
        }
    }

    private void handleLink(String data) {
        dataListener.showProgressDialog();

        compositeDisposable.add(
                contactsDataManager.acceptInvitation(data)
                        .flatMapCompletable(contact -> contactsDataManager.addContact(contact))
                        .andThen(contactsDataManager.saveContacts())
                        .andThen(contactsDataManager.getContactList())
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .toList()
                        .subscribe(
                                contacts -> {
                                    handleContactListUpdate(contacts);
                                    dataListener.showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_GENERAL);
                                }, throwable -> dataListener.showToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));

    }

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
//                            dataListener.showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
//                        }, throwable -> dataListener.showToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));
//    }
}
