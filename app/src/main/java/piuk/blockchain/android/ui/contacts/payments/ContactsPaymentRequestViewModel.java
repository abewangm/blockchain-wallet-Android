package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;
import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import io.reactivex.Observable;
import org.apache.commons.lang3.NotImplementedException;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_ACCOUNT_POSITION;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_CONTACT_ID;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_REQUEST_TYPE;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_SATOSHIS;


@SuppressWarnings("WeakerAccess")
public class ContactsPaymentRequestViewModel extends BaseViewModel {

    private DataListener dataListener;
    private Contact recipient;
    private long satoshis;
    private int accountPosition;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PayloadManager payloadManager;
    private PaymentRequestType paymentRequestType;

    interface DataListener {

        Bundle getFragmentBundle();

        @Nullable
        String getNote();

        void finishPage();

        void contactLoaded(String name, PaymentRequestType paymentRequestType);

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
        String contactId = dataListener.getFragmentBundle().getString(ARGUMENT_CONTACT_ID);
        satoshis = dataListener.getFragmentBundle().getLong(ARGUMENT_SATOSHIS, -1L);
        accountPosition = dataListener.getFragmentBundle().getInt(ARGUMENT_ACCOUNT_POSITION, -1);
        paymentRequestType = (PaymentRequestType) dataListener.getFragmentBundle().getSerializable(ARGUMENT_REQUEST_TYPE);

        if (contactId != null && paymentRequestType != null && satoshis > -1L) {
            loadContact(contactId);
        } else {
            throw new AssertionError("Contact ID and PaymentRequestType must be passed to fragment");
        }
    }

    void sendRequest() {
        if (satoshis < 0) {
            dataListener.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
        } else {
            dataListener.showProgressDialog();

            if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {

                PaymentRequest paymentRequest = new PaymentRequest(satoshis, dataListener.getNote());

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
                RequestForPaymentRequest request = new RequestForPaymentRequest(satoshis, dataListener.getNote());
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

    private void loadContact(String contactId) {
        compositeDisposable.add(
                contactsDataManager.getContactList()
                        .filter(ContactsPredicates.filterById(contactId))
                        .subscribe(
                                contact -> {
                                    recipient = contact;
                                    dataListener.contactLoaded(recipient.getName(), paymentRequestType);
                                },
                                throwable -> {
                                    dataListener.showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                    dataListener.finishPage();
                                }));
    }

    private Observable<String> getNextReceiveAddress(int defaultIndex) {
        // TODO: 21/02/2017
        throw new NotImplementedException("");
//        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(defaultIndex));
    }

}
