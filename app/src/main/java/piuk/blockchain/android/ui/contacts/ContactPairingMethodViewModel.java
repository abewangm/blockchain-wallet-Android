package piuk.blockchain.android.ui.contacts;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.metadata.MetadataNodeFactory;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsManager;
import piuk.blockchain.android.data.services.ContactsService;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

import static piuk.blockchain.android.ui.contacts.ContactPairingMethodActivity.INTENT_KEY_CONTACT_NAME;

public class ContactPairingMethodViewModel extends BaseViewModel {

    private DataListener dataListener;
    private String contactName;
    @Inject AppUtil appUtil;
//    @Inject
    ContactsManager contactManager;

    interface DataListener {

        Intent getPageIntent();

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void finishActivityWithResult(int resultCode);

        void onShareIntentGenerated(Intent intent);

    }

    ContactPairingMethodViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {

        //
        // TODO: 15/12/2016  I bypassed injection here
        Contacts contacts = null;
        try {

            // TODO: 15/12/2016 prompt for second pw if any
            String secondPassword = null;

            PayloadManager pm = PayloadManager.getInstance();
            pm.loadNodes(secondPassword);
            MetadataNodeFactory fac = pm.getMetadataNodeFactory();
            contacts = new Contacts(fac.getMetadataNode(), fac.getSharedMetadataNode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContactsService contactsService = new ContactsService(contacts);
        contactManager = new ContactsManager(contactsService);


        Intent pageIntent = dataListener.getPageIntent();
        if (pageIntent != null && pageIntent.hasExtra(INTENT_KEY_CONTACT_NAME)) {
            // Don't need contact name in all flows to this page
            contactName = pageIntent.getStringExtra(INTENT_KEY_CONTACT_NAME);
        }
    }

    void handleScanInput(@NonNull String extra) {
        // TODO: 15/11/2016 Input validation?

        compositeDisposable.add(
                contactManager.acceptInvitation(extra)
                        .subscribe(
                                success -> {
                                    dataListener.onShowToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
                                    dataListener.finishActivityWithResult(Activity.RESULT_OK);
                                }, throwable -> dataListener.onShowToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)));
    }

    void onSendLinkClicked(Contact myDetails, Contact recipientDetails) {
        compositeDisposable.add(
                contactManager.createInvitation(myDetails, recipientDetails)
                .subscribe(
                        metaDataUri -> {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, metaDataUri.createURI());
                    intent.setType("text/plain");
                    dataListener.onShareIntentGenerated(intent);

                }, throwable -> dataListener.onShowToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

    }

    void onNfcClicked() {
        // TODO: 14/11/2016 Generate link and share via NFC
        /**
         * Although to be honest, using a URI for sharing will allow NFC use anyway, but it might
         * be good to make the option explicit?
         */
    }

//    private Observable<MetaDataUri> getUri(Contact contact) {
//        return contactManager.createInvitation(contact)
//                .map(invitation -> new MetaDataUri.Builder()
//                        .setUriType(MetaDataUri.UriType.INVITE)
//                        .setFrom("TEST USER")
//                        .setInviteId(invitation.getMdid())
//                        .create());
//
//    }

    boolean isCameraOpen() {
        return appUtil.isCameraOpen();
    }

}
