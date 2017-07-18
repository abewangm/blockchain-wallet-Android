package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.contacts.data.Contact;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;


public class ContactsInvitationBuilderPresenter extends BasePresenter<ContactsInvitationBuilderView> {

    private ContactsDataManager contactManager;
    @VisibleForTesting Contact recipient;
    @VisibleForTesting Contact sender;
    @VisibleForTesting String uri;

    @Inject
    ContactsInvitationBuilderPresenter(ContactsDataManager contactManager) {
        this.contactManager = contactManager;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void setNameOfSender(String nameOfSender) {
        sender = new Contact();
        sender.setName(nameOfSender);
    }

    void setNameOfRecipient(String nameOfRecipient) {
        recipient = new Contact();
        recipient.setName(nameOfRecipient);
    }

    void onQrCodeSelected() {
        if (uri == null) {
            getView().showProgressDialog();

            getCompositeDisposable().add(
                    contactManager.createInvitation(sender, recipient)
                            .map(Contact::createURI)
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(
                                    uri -> {
                                        this.uri = uri;
                                        getView().onUriGenerated(uri, recipient.getName());
                                    },
                                    throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
        } else {
            // Prevents contact being added more than once, as well as unnecessary web calls
            getView().onUriGenerated(uri, recipient.getName());
        }
    }

    void onLinkClicked() {
        if (uri == null) {
            getView().showProgressDialog();

            getCompositeDisposable().add(
                    contactManager.createInvitation(sender, recipient)
                            .map(Contact::createURI)
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(
                                    uri -> {
                                        this.uri = uri;
                                        generateIntent(uri);
                                    },
                                    throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
        } else {
            // Prevents contact being added more than once, as well as unnecessary web calls
            generateIntent(uri);
        }
    }

    void onDoneSelected() {
        if (uri == null) {
            getView().finishPage();
        } else {
            // Check status of sent invitation
            getView().showProgressDialog();
            getCompositeDisposable().add(
                    contactManager.readInvitationSent(recipient)
                            .doAfterTerminate(() -> getView().dismissProgressDialog())
                            .subscribe(
                                    success -> getView().finishPage(),
                                    throwable -> getView().finishPage()));
        }
    }

    private void generateIntent(String uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, uri);
        intent.setType("text/plain");
        getView().onLinkGenerated(intent);
    }
}
