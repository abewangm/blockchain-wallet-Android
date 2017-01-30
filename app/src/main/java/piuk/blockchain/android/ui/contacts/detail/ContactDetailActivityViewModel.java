package piuk.blockchain.android.ui.contacts.detail;

import android.support.annotation.StringRes;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactsPredicates;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;


@SuppressWarnings("WeakerAccess")
public class ContactDetailActivityViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject ContactsDataManager contactsDataManager;

    interface DataListener {

        void showProgressDialog(@StringRes int string);

        void dismissProgressDialog();

        void showBroadcastFailedDialog(String mdid, String txHash, String facilitatedTxId, long transactionValue);

        void showBroadcastSuccessDialog();

        void dismissPaymentPage();

        void showPaymentMismatchDialog(@StringRes int message);
    }

    ContactDetailActivityViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void broadcastPaymentSuccess(String mdid, String txHash, String facilitatedTxId, long transactionValue) {
        dataListener.showProgressDialog(R.string.contacts_broadcasting_payment);

        compositeDisposable.add(
                // Get contacts
                contactsDataManager.getContactList()
                        // Find contact by MDID
                        .filter(ContactsPredicates.filterByMdid(mdid))
                        // Get FacilitatedTransaction from HashMap
                        .flatMap(contact -> Observable.just(contact.getFacilitatedTransaction().get(facilitatedTxId)))
                        // Check the payment value was appropriate
                        .flatMapCompletable(transaction -> {
                            // Too much sent
                            if (transactionValue > transaction.getIntended_amount()) {
                                dataListener.showPaymentMismatchDialog(R.string.contacts_too_much_sent);
                                return Completable.complete();
                                // Too little sent
                            } else if (transactionValue < transaction.getIntended_amount()) {
                                dataListener.showPaymentMismatchDialog(R.string.contacts_too_little_sent);
                                return Completable.complete();
                                // Correct amount sent
                            } else {
                                // Broadcast payment to shared metadata service
                                return contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                                        // Show successfully broadcast
                                        .doOnComplete(() -> dataListener.showBroadcastSuccessDialog())
                                        // Show retry dialog if broadcast failed
                                        .doOnError(throwable -> dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId, transactionValue));
                            }
                        })
                        // Dismiss send page on termination
                        .doAfterTerminate(() -> {
                            dataListener.dismissPaymentPage();
                            dataListener.dismissProgressDialog();
                        })
                        .subscribe(
                                () -> {
                                    // No-op
                                }, throwable -> {
                                    // Not sure if it's worth notifying people at this point? Dialogs are advisory anyway.
                                }));
    }

}
