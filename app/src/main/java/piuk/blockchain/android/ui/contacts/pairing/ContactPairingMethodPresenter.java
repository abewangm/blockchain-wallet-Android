package piuk.blockchain.android.ui.contacts.pairing;

import android.app.Activity;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

public class ContactPairingMethodPresenter extends BasePresenter<ContactsPairingMethodView> {

    private AppUtil appUtil;
    private ContactsDataManager contactManager;

    @Inject
    ContactPairingMethodPresenter(AppUtil appUtil, ContactsDataManager contactManager) {
        this.appUtil = appUtil;
        this.contactManager = contactManager;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void handleScanInput(@NonNull String extra) {
        getCompositeDisposable().add(
                contactManager.acceptInvitation(extra)
                        .subscribe(
                                contact -> {
                                    getView().showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
                                    getView().finishActivityWithResult(Activity.RESULT_OK);
                                }, throwable -> getView().showToast(R.string.contacts_invalid_qr, ToastCustom.TYPE_ERROR)));
    }

    boolean isCameraOpen() {
        return appUtil.isCameraOpen();
    }

}
