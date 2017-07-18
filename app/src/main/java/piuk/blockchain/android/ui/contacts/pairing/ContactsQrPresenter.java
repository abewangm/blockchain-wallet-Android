package piuk.blockchain.android.ui.contacts.pairing;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.data.notifications.models.NotificationPayload;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;


public class ContactsQrPresenter extends BasePresenter<ContactsQrView> {

    @VisibleForTesting static final int DIMENSION_QR_CODE = 600;

    private Observable<NotificationPayload> notificationObservable;
    private QrCodeDataManager qrCodeDataManager;
    private NotificationManager notificationManager;
    private RxBus rxBus;

    @Inject
    ContactsQrPresenter(QrCodeDataManager qrCodeDataManager,
                        NotificationManager notificationManager,
                        RxBus rxBus) {

        this.qrCodeDataManager = qrCodeDataManager;
        this.notificationManager = notificationManager;
        this.rxBus = rxBus;
    }

    @Override
    public void onViewReady() {
        subscribeToNotifications();

        Bundle fragmentBundle = getView().getFragmentBundle();
        if (fragmentBundle != null) {
            String name = fragmentBundle.getString(ContactsInvitationBuilderQrFragment.ARGUMENT_NAME);
            getView().updateDisplayMessage(name);
            String uri = fragmentBundle.getString(ContactsInvitationBuilderQrFragment.ARGUMENT_URI);

            getCompositeDisposable().add(
                    qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                            .subscribe(
                                    bitmap -> getView().onQrLoaded(bitmap),
                                    throwable -> getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        } else {
            getView().showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        }
    }

    private void subscribeToNotifications() {
        notificationObservable = rxBus.register(NotificationPayload.class);
        getCompositeDisposable().add(
                notificationObservable
                        .subscribe(
                                notificationPayload -> {
                                    if (notificationPayload.getType() != null
                                            && notificationPayload.getType().equals(NotificationPayload.NotificationType.CONTACT_REQUEST)) {
                                        // TODO: 31/01/2017 Currently neither of these work for some reason
                                        notificationManager.cancel(FcmCallbackService.ID_FOREGROUND_NOTIFICATION);
                                        getView().finishPage();
                                    }
                                },
                                throwable -> {
                                    // No-op
                                }
                        ));
    }

    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        rxBus.unregister(NotificationPayload.class, notificationObservable);
    }

}
