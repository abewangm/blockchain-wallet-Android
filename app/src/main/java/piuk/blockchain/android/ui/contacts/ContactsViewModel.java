package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import info.blockchain.wallet.metadata.data.PaymentRequest;
import info.blockchain.wallet.metadata.data.Trusted;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.MetaDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.metadata.MetaDataUri;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;

import static piuk.blockchain.android.ui.contacts.ContactsActivity.EXTRA_METADATA_URI;

@SuppressWarnings("WeakerAccess")
public class ContactsViewModel extends BaseViewModel {

    private static final String TAG = ContactsViewModel.class.getSimpleName();
    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject MetaDataManager metaDataManager;
    @Inject PrefsUtil prefsUtil;

    interface DataListener {

        Intent getPageIntent();

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void showQrCode(@NonNull Bitmap bitmap);

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void setUiState(@ContactsActivity.UiState int uiState);

        void showProgressDialog();

        void dismissProgressDialog();

        void showAddContactConfirmation(String name, DialogButtonCallback dialogButtonCallback);

    }

    ContactsViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // Subscribe to notification events
        subscribeToNotifications();

        dataListener.setUiState(ContactsActivity.LOADING);
        compositeDisposable.add(
                metaDataManager.getTrustedList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> {
                                    Log.e(TAG, "onViewReady: ", throwable);
                                    dataListener.setUiState(ContactsActivity.FAILURE);
                                }));

        // TODO: 16/11/2016 Move me to my own function. I will likely need to be called from system-wide broadcasts
        // I'm only here for testing purposes
        compositeDisposable.add(
                metaDataManager.getPaymentRequests(true)
                        .subscribe(
                                messages -> {
                                    Log.d(TAG, "onViewReady: " + messages);
                                },
                                throwable -> {
                                    Log.e(TAG, "onViewReady: ", throwable);
                                    dataListener.onShowToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                }));

        Intent intent = dataListener.getPageIntent();
        if (intent != null && intent.hasExtra(EXTRA_METADATA_URI)) {
            MetaDataUri uriObject = MetaDataUri.decode(intent.getStringExtra(EXTRA_METADATA_URI));
            handleIncomingUri(uriObject);
        }
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

        compositeDisposable.add(
                metaDataManager.sendPaymentRequest(mdid, paymentRequest)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                message -> Log.d(TAG, "onRequestMoneyClicked: " + message),
                                throwable -> {
                                    dataListener.onShowToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR);
                                    Log.e(TAG, "onRequestMoneyClicked: ", throwable);
                                }));

        // User should get back a notification with a payment request response
        // Download response from getPaymentRequestResponses(...)
    }

    void onRequestMoneyClicked(String mdid) {

    }

    void onRenameContactClicked(String mdid) {

    }

    void onDeleteContactClicked(String mdid) {
        dataListener.showProgressDialog();
        compositeDisposable.add(
                metaDataManager.deleteTrusted(mdid)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(success -> {
                            if (success) {
                                onViewReady();
                            } else {
                                dataListener.onShowToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR);
                            }
                        }, throwable -> dataListener.onShowToast(R.string.contacts_delete_contact_failed, ToastCustom.TYPE_ERROR)));
    }

    // TODO: 16/11/2016

    /**
     * This will need to subscribe to the notification service somehow and listen for when the
     * recipient accepts their invitation. Once this is done, the dialog will need to be dismissed
     * and the user will have to make a call to {@link piuk.blockchain.android.data.services.SharedMetaDataService#putTrusted(String,
     * String)} to add them as a contact themselves
     */
    void onViewQrClicked() {
        dataListener.showProgressDialog();

        compositeDisposable.add(
                metaDataManager.createInvitation()
                        .flatMap(share -> getMetaDataUriString(share.getId()))
                        .flatMap(uri -> qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE))
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                bitmap -> dataListener.showQrCode(bitmap),
                                throwable -> dataListener.onShowToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    private Observable<String> getMetaDataUriString(String invitationId) {
        MetaDataUri uri = new MetaDataUri.Builder()
                .setFrom("TEST USER")
                .setInviteId(invitationId)
                .setUriType(MetaDataUri.UriType.INVITE)
                .create();

        return Observable.just(uri.encode().toString());
    }

    private void handleContactListUpdate(Trusted trusted) {
        ArrayList<ContactsListItem> list = new ArrayList<>();

        for (String contact : trusted.getContacts()) {
            list.add(new ContactsListItem(contact, contact, "Trusted"));
        }

        if (!list.isEmpty()) {
            dataListener.setUiState(ContactsActivity.CONTENT);
            dataListener.onContactsLoaded(list);
        } else {
            dataListener.setUiState(ContactsActivity.EMPTY);
        }
    }

    private void handleIncomingUri(MetaDataUri metaDataUri) {
        String name = metaDataUri.getFrom();
        dataListener.showAddContactConfirmation(name, new DialogButtonCallback() {
            @Override
            public void onPositiveClicked() {
                addUser(metaDataUri);
            }

            @Override
            public void onNegativeClicked() {
                // Ignore
            }
        });
    }

    @Thunk
    void addUser(MetaDataUri metaDataUri) {
        String name = metaDataUri.getFrom();
        String inviteId = metaDataUri.getInviteId();

        dataListener.showProgressDialog();

        compositeDisposable.add(
                metaDataManager.acceptInvitation(inviteId)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .flatMap(invitation -> metaDataManager.getTrustedList())
                        .subscribe(trusted -> {
                            handleContactListUpdate(trusted);
                            dataListener.onShowToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
                        }, throwable -> dataListener.onShowToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));
    }
}
