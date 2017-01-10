package piuk.blockchain.android.ui.contacts;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

public class ContactPairingMethodViewModel extends BaseViewModel {

    private DataListener dataListener;
    private String contactName;
    @Inject AppUtil appUtil;
    @Inject ContactsDataManager contactManager;

    interface DataListener {

        Intent getPageIntent();

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void finishActivityWithResult(int resultCode);

        void onShareIntentGenerated(Intent intent);

    }

    ContactPairingMethodViewModel(DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
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
                            // FIXME java.lang.ClassNotFoundException: Didn't find class "org.apache.http.client.utils.URIBuilder" on path
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
