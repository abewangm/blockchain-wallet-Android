package piuk.blockchain.android.ui.contacts.list;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.exceptions.DecryptionException;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.notifications.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.customviews.ToastCustom;

@SuppressWarnings("WeakerAccess")
public class ContactsListViewModel extends BaseViewModel {

    private static final String TAG = ContactsListViewModel.class.getSimpleName();

    private DataListener dataListener;
    private Observable<NotificationPayload> notificationObservable;
    @VisibleForTesting String link;
    @Inject protected ContactsDataManager contactsDataManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected RxBus rxBus;

    interface DataListener {

        Intent getPageIntent();

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void setUiState(@UiState.UiStateDef int uiState);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        void showSecondPasswordDialog();

    }

    ContactsListViewModel(DataListener dataListener) {
        Injector.getInstance().getPresenterComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // Subscribe to notification events
        subscribeToNotifications();
        // Set up page if Metadata initialized
        attemptPageSetup(true);

        Intent intent = dataListener.getPageIntent();
        if (intent != null && intent.hasExtra(ContactsListActivity.EXTRA_METADATA_URI)) {
            link = intent.getStringExtra(ContactsListActivity.EXTRA_METADATA_URI);
            intent.removeExtra(ContactsListActivity.EXTRA_METADATA_URI);
        }
    }

    void initContactsService(@Nullable String secondPassword) {
        dataListener.setUiState(UiState.LOADING);
        compositeDisposable.add(
                payloadDataManager.generateNodes(secondPassword)
                        .andThen(payloadDataManager.getMetadataNodeFactory())
                        .flatMapCompletable(metadataNodeFactory -> contactsDataManager.initContactsService(
                                metadataNodeFactory.getMetadataNode(),
                                metadataNodeFactory.getSharedMetadataNode()))
                        .subscribe(this::registerMdid,
                                throwable -> {
                                    dataListener.setUiState(UiState.FAILURE);
                                    if (throwable instanceof DecryptionException) {
                                        dataListener.showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR);
                                    } else {
                                        dataListener.showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                    }
                                }));
    }

    // TODO: 30/03/2017 Move this into the registerNodeForMetaDataService function
    private void registerMdid() {
        compositeDisposable.add(
                payloadDataManager.registerMdid()
                        .flatMapCompletable(responseBody -> contactsDataManager.publishXpub())
                        .subscribe(
                                () -> attemptPageSetup(false),
                                throwable -> {
                                    dataListener.setUiState(UiState.FAILURE);
                                    dataListener.showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                }));
    }

    @VisibleForTesting
    void refreshContacts() {
        dataListener.setUiState(UiState.LOADING);
        compositeDisposable.add(
                contactsDataManager.fetchContacts()
                        .andThen(contactsDataManager.getContactList())
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> dataListener.setUiState(UiState.FAILURE)));
    }

    private void loadContacts() {
        dataListener.setUiState(UiState.LOADING);
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> dataListener.setUiState(UiState.FAILURE)));
    }

    // TODO: 19/01/2017 This is pretty gross and I'm certain it can be Rx-ified in the future
    private void handleContactListUpdate(List<Contact> contacts) {
        List<ContactsListItem> list = new ArrayList<>();
        List<Contact> pending = new ArrayList<>();

        compositeDisposable.add(
                contactsDataManager.getContactsWithUnreadPaymentRequests()
                        .toList()
                        .subscribe(actionRequired -> {
                            for (Contact contact : contacts) {
                                list.add(new ContactsListItem(
                                        contact.getId(),
                                        contact.getName(),
                                        contact.getMdid() != null && !contact.getMdid().isEmpty()
                                                ? ContactsListItem.Status.TRUSTED
                                                : ContactsListItem.Status.PENDING,
                                        contact.getCreated(),
                                        isInList(actionRequired, contact)));

                                if (contact.getMdid() == null || contact.getMdid().isEmpty()) {
                                    pending.add(contact);
                                }
                            }

                            checkStatusOfPendingContacts(pending);
                            updateUI(list);

                        }, throwable -> {
                            dataListener.onContactsLoaded(new ArrayList<>());
                            dataListener.setUiState(UiState.FAILURE);
                        }));
    }

    private void updateUI(List<ContactsListItem> list) {
        if (!list.isEmpty()) {
            dataListener.setUiState(UiState.CONTENT);
            dataListener.onContactsLoaded(list);
        } else {
            dataListener.onContactsLoaded(new ArrayList<>());
            dataListener.setUiState(UiState.EMPTY);
        }
    }

    private boolean isInList(List<Contact> contacts, Contact toBeFound) {
        for (Contact contact : contacts) {
            if (contact.getId().equals(toBeFound.getId())) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    void checkStatusOfPendingContacts(List<Contact> pending) {
        for (int i = 0; i < pending.size(); i++) {
            final Contact contact = pending.get(i);
            compositeDisposable.add(
                    contactsDataManager.readInvitationSent(contact)
                            .subscribe(
                                    success -> {
                                        if (success) loadContacts();
                                    },
                                    throwable -> {
                                        // No-op
                                    }));
        }
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);

        compositeDisposable.add(
                notificationObservable
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> refreshContacts(),
                                throwable -> Log.e(TAG, "subscribeToNotifications: ", throwable)));
    }

    @VisibleForTesting
    void handleLink(String data) {
        dataListener.showProgressDialog();
        compositeDisposable.add(
                contactsDataManager.acceptInvitation(data)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                contact -> {
                                    link = null;
                                    refreshContacts();
                                    dataListener.showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
                                }, throwable -> dataListener.showToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));

    }

    private void attemptPageSetup(boolean firstAttempt) {
        if (firstAttempt) {
            dataListener.setUiState(UiState.LOADING);
            compositeDisposable.add(
                    payloadDataManager.loadNodes()
                            .subscribe(
                                    success -> {
                                        if (success) {
                                            if (link != null) {
                                                handleLink(link);
                                            } else {
                                                refreshContacts();
                                            }
                                        } else {
                                            // Not set up, most likely has a second password enabled
                                            if (payloadDataManager.isDoubleEncrypted()) {
                                                dataListener.showSecondPasswordDialog();
                                                dataListener.setUiState(UiState.FAILURE);
                                            } else {
                                                initContactsService(null);
                                            }
                                        }
                                    }, throwable -> dataListener.setUiState(UiState.FAILURE)));
        } else {
            dataListener.setUiState(UiState.FAILURE);
        }
    }

    @Override
    public void destroy() {
        rxBus.unregister(NotificationPayload.class, notificationObservable);
        super.destroy();
    }
}
