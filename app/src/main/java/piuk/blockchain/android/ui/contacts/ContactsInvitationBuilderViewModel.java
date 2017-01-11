package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactsInvitationBuilderViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    private String nameOfRecipient;
    private String nameOfSender;
    @Inject ContactsDataManager contactManager;
    @Inject QrCodeDataManager qrCodeDataManager;

    interface DataListener {

        void showProgressDialog();

        void dismissProgressDialog();

        void onQrCodeLoaded(Bitmap bitmap, String nameOfRecipient);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onLinkGenerated(Intent intent);
    }

    ContactsInvitationBuilderViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void setNameOfSender(String nameOfSender) {
        this.nameOfSender = nameOfSender;
    }

    void setNameOfRecipient(String nameOfRecipient) {
        this.nameOfRecipient = nameOfRecipient;
    }

    void onViewQrClicked() {
        dataListener.showProgressDialog();

        Contact sender = new Contact();
        Contact recipient = new Contact();
        sender.setName(nameOfSender);
        recipient.setName(nameOfRecipient);

        compositeDisposable.add(
                contactManager.createInvitation(sender, recipient)
                        .map(Contact::createURI)
                        .flatMap(uri -> qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE))
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                bitmap -> dataListener.onQrCodeLoaded(bitmap, nameOfRecipient),
                                throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }

    void onLinkClicked() {
        dataListener.showProgressDialog();

        Contact sender = new Contact();
        Contact recipient = new Contact();
        sender.setName(nameOfSender);
        recipient.setName(nameOfRecipient);

        compositeDisposable.add(
                contactManager.createInvitation(sender, recipient)
                        .map(Contact::createURI)
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(
                                uri -> {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_SEND);
                                    intent.putExtra(Intent.EXTRA_TEXT, intent);
                                    intent.setType("text/plain");
                                    dataListener.onLinkGenerated(intent);
                                },
                                throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }
}
