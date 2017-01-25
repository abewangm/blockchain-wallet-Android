package piuk.blockchain.android.ui.contacts.detail;

import android.support.annotation.StringRes;

import info.blockchain.wallet.exceptions.MismatchValueException;

import javax.inject.Inject;

import piuk.blockchain.android.R;
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
                contactsDataManager.sendPaymentBroadcasted(mdid, txHash, facilitatedTxId)
                        .doAfterTerminate(() -> {
                            dataListener.dismissPaymentPage();
                            dataListener.dismissProgressDialog();
                        })
                        .subscribe(
                                () -> dataListener.onShowToast(R.string.contacts_payment_sent_success, ToastCustom.TYPE_OK),
                                throwable -> {
                                    if (throwable instanceof MismatchValueException) {
                                        // Show warning that amount wasn't enough
                                        // TODO: 24/01/2017 Implement me
                                        dataListener.onShowToast(R.string.not_sane_error, ToastCustom.TYPE_OK);
                                    } else {
                                        dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId);
                                    }
                                }));
    }
}
