package piuk.blockchain.android.ui.contacts.detail;

import android.support.annotation.StringRes;

import javax.inject.Inject;

import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactDetailActivityViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject ContactsDataManager contactsDataManager;

    interface DataListener {

        void showProgressDialog(@StringRes int string);

        void dismissProgressDialog();

        void onShowToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId);

        void dismissPaymentPage();

        void showPaymentMismatchDialog(@StringRes int message);
    }

    ContactDetailActivityViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {

    }

    void broadcastPaymentSuccess(String mdid, String txHash, String facilitatedTxId) {
        dataListener.showProgressDialog(R.string.contacts_broadcasting_payment);

        compositeDisposable.add(
                // Broadcast payment to shared metadata service
                contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                        // Dismiss send page on termination
                        .doAfterTerminate(() -> {
                            dataListener.dismissPaymentPage();
                            dataListener.dismissProgressDialog();
                        })
                        // Show successfully broadcast
                        .doOnComplete(() -> dataListener.onShowToast(R.string.contacts_payment_sent_success, ToastCustom.TYPE_OK))
                        // Show retry dialog if broadcast failed
                        .doOnError(throwable -> dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId))
                        // Get contacts
                        .andThen(contactsDataManager.getContactList())
                        // Find contact by MDID
                        .filter(ContactsPredicates.filterByMdid(mdid))
                        // Get FacilitatedTransaction from HashMap
                        .flatMap(contact -> Observable.just(contact.getFacilitatedTransaction().get(facilitatedTxId)))
                        // Check the payment value was appropriate
                        .flatMap(transaction -> contactsDataManager.checkPaymentValue(transaction, mdid))
                        .subscribe(
                                integer -> {
                                    switch (integer) {
                                        case -1:
                                            // Too little was sent
                                            dataListener.showPaymentMismatchDialog(R.string.contacts_too_little_sent);
                                            break;
                                        case 0:
                                            // Correct amount sent, don't need to do anything here
                                            break;
                                        case 1:
                                            // Too much
                                            dataListener.showPaymentMismatchDialog(R.string.contacts_too_much_sent);
                                            break;
                                        default:
                                            // Something is broken, not sure if it's worth notifying people at this point?
                                            break;
                                    }
                                }, throwable -> {
                                    // Not sure if it's worth notifying people at this point? Dialogs are advisory anyway.
                                }
                        ));
    }
}
