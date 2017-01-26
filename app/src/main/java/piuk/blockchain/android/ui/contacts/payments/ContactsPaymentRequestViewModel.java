package piuk.blockchain.android.ui.contacts.payments;

import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.PrefsUtil;


@SuppressWarnings("WeakerAccess")
public class ContactsPaymentRequestViewModel extends BaseViewModel {

    private DataListener dataListener;
    private Contact recipient;
    private String note;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PrefsUtil prefsUtil;
    @Inject PayloadManager payloadManager;

    interface DataListener {

        PaymentRequestType getPaymentRequestType();

        void finishPage();

        void contactLoaded();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        void showSendSuccessfulDialog(String name);

        void showRequestSuccessfulDialog();
    }

    ContactsPaymentRequestViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void loadContact(String contactId) {
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .filter(ContactsPredicates.filterById(contactId))
                        .subscribe(
                                contact -> {
                                    recipient = contact;
                                    dataListener.contactLoaded();
                                },
                                throwable -> {
                                    dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                    dataListener.finishPage();
                                }));
    }

    String getContactName() {
        return recipient.getName();
    }

    void onNoteSet(String note) {
        this.note = note;
    }

    void onAmountSet(long satoshis, int accountPosition) {
        if (satoshis < 0) {
            dataListener.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
        } else {
            dataListener.showProgressDialog();

            if (dataListener.getPaymentRequestType().equals(PaymentRequestType.REQUEST)) {

                PaymentRequest paymentRequest = new PaymentRequest(satoshis, note);

                compositeDisposable.add(
                        getNextReceiveAddress(accountPosition)
                                .doOnNext(paymentRequest::setAddress)
                                // Request that the other person sends payment
                                .flatMapCompletable(s -> contactsDataManager.requestSendPayment(recipient.getMdid(), paymentRequest))
                                .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                                .subscribe(
                                        () -> dataListener.showSendSuccessfulDialog(recipient.getName()),
                                        throwable -> dataListener.showToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR)));

            } else {
                RequestForPaymentRequest request = new RequestForPaymentRequest(satoshis, note);
                compositeDisposable.add(
                        // Request that the other person receives payment
                        contactsDataManager.requestReceivePayment(recipient.getMdid(), request)
                                .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                                .subscribe(
                                        () -> dataListener.showRequestSuccessfulDialog(),
                                        throwable -> dataListener.showToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR)));
            }
        }
    }

    private Observable<String> getNextReceiveAddress(int defaultIndex) {
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(defaultIndex));
    }

}
