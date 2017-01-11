package piuk.blockchain.android.ui.contacts;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

import static piuk.blockchain.android.ui.contacts.ContactsInvitationBuilderQrFragment.KEY_BUNDLE_NAME;
import static piuk.blockchain.android.ui.contacts.ContactsInvitationBuilderQrFragment.KEY_BUNDLE_URI;


@SuppressWarnings("WeakerAccess")
public class ContactsQrViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;

    interface DataListener {

        Bundle getFragmentBundle();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onQrLoaded(Bitmap bitmap);

        void updateDisplayMessage(String name);

    }

    ContactsQrViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        if (dataListener.getFragmentBundle() != null) {

            String name = dataListener.getFragmentBundle().getString(KEY_BUNDLE_NAME);
            dataListener.updateDisplayMessage(name);
            String uri = dataListener.getFragmentBundle().getString(KEY_BUNDLE_URI);

            compositeDisposable.add(
                    qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                            .subscribe(
                                    bitmap -> dataListener.onQrLoaded(bitmap),
                                    throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        } else {
            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        }
    }
}
