package piuk.blockchain.android.ui.contacts.payments;

import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
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

        void finishPage();

        void contactLoaded();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        PaymentRequestType getPaymentRequestType();

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

    void onAmountSet(long satoshis) {
        if (satoshis < 0) {
            dataListener.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
        } else {
            dataListener.showProgressDialog();

            if (dataListener.getPaymentRequestType().equals(PaymentRequestType.SEND)) {

                PaymentRequest request = new PaymentRequest(satoshis, note);
                FacilitatedTransaction facilitatedTransaction = new FacilitatedTransaction();
                facilitatedTransaction.setIntended_amount(satoshis);
                int defaultAccount = payloadManager.getPayload().getHdWallet().getDefaultIndex();

                compositeDisposable.add(
                        getNextReceiveAddress(defaultAccount)
                                .doOnNext(facilitatedTransaction::setAddress)
                                .flatMapCompletable(s -> contactsDataManager.requestSendPayment(recipient.getMdid(), request, facilitatedTransaction))
                                .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                                .subscribe(
                                        () -> dataListener.showSendSuccessfulDialog(recipient.getName()),
                                        throwable -> dataListener.showToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR)));
            } else {
                RequestForPaymentRequest request = new RequestForPaymentRequest(satoshis, note);

                compositeDisposable.add(
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
