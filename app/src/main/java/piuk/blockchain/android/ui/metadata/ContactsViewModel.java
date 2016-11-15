package piuk.blockchain.android.ui.metadata;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ContactsViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;

    interface DataListener {

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void showContactsLoadingFailed();

        void showQrCode(@NonNull Bitmap bitmap);

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

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

    }

    void onViewQrClicked() {
        // TODO: 14/11/2016 Generate correct URI for user
        String uri = "???";
        compositeDisposable.add(
                qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                        .subscribe(
                                bitmap -> dataListener.showQrCode(bitmap),
                                throwable -> dataListener.onShowToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    @Override
    public void onViewReady() {
        // TODO: 14/11/2016 Load users

        ContactsListItem user1 = new ContactsListItem("mdid:12345", "Adam", "Waiting for confirmation");
        ContactsListItem user2 = new ContactsListItem("mdid:67890", "Riaan", "Trusted");
        ContactsListItem user3 = new ContactsListItem("mdid:11111", "Matt", "Trusted");
        ContactsListItem user4 = new ContactsListItem("mdid:22222", "Sjors", "Trusted");
        ContactsListItem user5 = new ContactsListItem("mdid:33333", "Mats", "Waiting for confirmation");
        ContactsListItem user6 = new ContactsListItem("mdid:44444", "Antoine", "French");

        dataListener.onContactsLoaded(Arrays.asList(user1, user2, user3, user4, user5, user6));
    }
}
