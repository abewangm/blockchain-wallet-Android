package piuk.blockchain.android.ui.contacts.payments;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.contacts.data.PaymentRequest;
import info.blockchain.wallet.contacts.data.RequestForPaymentRequest;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;

import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_ACCOUNT_POSITION;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_CONTACT_ID;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_REQUEST_TYPE;
import static piuk.blockchain.android.ui.contacts.payments.ContactPaymentRequestNotesFragment.ARGUMENT_SATOSHIS;


public class ContactsPaymentRequestPresenter extends BasePresenter<ContactPaymentRequestView> {

    private ContactsDataManager contactsDataManager;
    private PayloadDataManager payloadDataManager;
    @VisibleForTesting Contact recipient;
    @VisibleForTesting long satoshis;
    @VisibleForTesting int accountPosition;
    @VisibleForTesting PaymentRequestType paymentRequestType;

    @Inject
    ContactsPaymentRequestPresenter(ContactsDataManager contactsDataManager,
                                    PayloadDataManager payloadDataManager) {

        this.contactsDataManager = contactsDataManager;
        this.payloadDataManager = payloadDataManager;
    }

    @Override
    public void onViewReady() {
        Bundle fragmentBundle = getView().getFragmentBundle();
        String contactId = fragmentBundle.getString(ARGUMENT_CONTACT_ID);
        satoshis = fragmentBundle.getLong(ARGUMENT_SATOSHIS, -1L);
        accountPosition = fragmentBundle.getInt(ARGUMENT_ACCOUNT_POSITION, -1);
        paymentRequestType = (PaymentRequestType) fragmentBundle.getSerializable(ARGUMENT_REQUEST_TYPE);

        if (contactId != null && paymentRequestType != null && satoshis > -1L) {
            loadContact(contactId);
        } else {
            throw new AssertionError("Contact ID and PaymentRequestType must be passed to fragment");
        }
    }

    void sendRequest() {
        if (satoshis <= 0) {
            getView().showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR);
        } else {
            getView().showProgressDialog();

            if (paymentRequestType.equals(PaymentRequestType.REQUEST)) {

                PaymentRequest paymentRequest = new PaymentRequest(satoshis, getView().getNote());

                getCompositeDisposable().add(
                        payloadDataManager.getNextReceiveAddress(accountPosition)
                                .doOnNext(paymentRequest::setAddress)
                                // Request that the other person sends payment
                                .flatMapCompletable(s -> contactsDataManager.requestSendPayment(recipient.getMdid(), paymentRequest))
                                .doAfterTerminate(() -> getView().dismissProgressDialog())
                                .subscribe(
                                        () -> getView().showSendSuccessfulDialog(recipient.getName()),
                                        throwable -> getView().showToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR)));

            } else {
                RequestForPaymentRequest request = new RequestForPaymentRequest(satoshis, getView().getNote());
                getCompositeDisposable().add(
                        // Request that the other person receives payment
                        contactsDataManager.requestReceivePayment(recipient.getMdid(), request)
                                .doAfterTerminate(() -> getView().dismissProgressDialog())
                                .subscribe(
                                        () -> getView().showRequestSuccessfulDialog(),
                                        throwable -> getView().showToast(R.string.contacts_error_sending_payment_request, ToastCustom.TYPE_ERROR)));
            }
        }
    }

    private void loadContact(String contactId) {
        getCompositeDisposable().add(
                contactsDataManager.getContactList()
                        .filter(ContactsPredicates.filterById(contactId))
                        .subscribe(
                                contact -> {
                                    recipient = contact;
                                    getView().contactLoaded(recipient.getName(), paymentRequestType);
                                },
                                throwable -> {
                                    getView().showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                    getView().finishPage();
                                },
                                () -> {
                                    if (recipient == null) {
                                        // Wasn't found via filter, show not found
                                        getView().showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR);
                                        getView().finishPage();
                                    }
                                }));
    }

}
