package piuk.blockchain.android.ui.contacts;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.MetaDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ContactsViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject MetaDataManager metaDataManager;

    interface DataListener {

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void showQrCode(@NonNull Bitmap bitmap);

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void setUiState(ContactsActivity.UI_STATE uiState);

        void showProgressDialog();

        void dismissProgressDialog();

    }

    ContactsViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    void onSendMoneyClicked(String mdid) {

    }

    void onRequestMoneyClicked(String mdid) {

    }

    void onRenameContactClicked(String mdid) {

    }

    void onDeleteContactClicked(String mdid) {
        dataListener.showProgressDialog();
        compositeDisposable.add(
                metaDataManager.deleteInvitation(mdid)
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
                        .flatMap(share -> qrCodeDataManager.generateQrCode(share.getId(), DIMENSION_QR_CODE))
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                bitmap -> dataListener.showQrCode(bitmap),
                                throwable -> dataListener.onShowToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    private static final String TAG = ContactsViewModel.class.getSimpleName();

    @Override
    public void onViewReady() {
        dataListener.setUiState(ContactsActivity.UI_STATE.LOADING);
        compositeDisposable.add(
                metaDataManager.getTrustedList()
                        .subscribe(trusted -> {
                            ArrayList<ContactsListItem> list = new ArrayList<>();

                            for (String contact : trusted.getContacts()) {
                                list.add(new ContactsListItem(contact, contact, "Trusted"));
                            }

                            if (!list.isEmpty()) {
                                dataListener.setUiState(ContactsActivity.UI_STATE.CONTENT);
                                dataListener.onContactsLoaded(list);
                            } else {
                                dataListener.setUiState(ContactsActivity.UI_STATE.EMPTY);
                            }

                        }, throwable -> {
                            Log.e(TAG, "onViewReady: ", throwable);
                            dataListener.setUiState(ContactsActivity.UI_STATE.FAILURE);
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
    }
}
