package piuk.blockchain.android.ui.contacts;

import android.content.Intent;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactsInvitationBuilderViewModel extends BaseViewModel {

    private DataListener dataListener;
    private String nameOfRecipient;
    private String nameOfSender;
    @Inject ContactsDataManager contactManager;

    interface DataListener {

        void showProgressDialog();

        void dismissProgressDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onLinkGenerated(Intent intent);

        void onUriGenerated(String uri, String recipientName);

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

    void onQrCodeSelected() {
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
                                uri -> dataListener.onUriGenerated(uri, nameOfRecipient),
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
                                    intent.putExtra(Intent.EXTRA_TEXT, uri);
                                    intent.setType("text/plain");
                                    dataListener.onLinkGenerated(intent);
                                },
                                throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
    }
}
