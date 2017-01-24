package piuk.blockchain.android.ui.contacts.detail;

import android.support.annotation.StringRes;

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
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .subscribe(() -> dataListener.onShowToast(R.string.contacts_payment_sent_success, ToastCustom.TYPE_OK),
                                throwable -> dataListener.showBroadcastFailedDialog(mdid, txHash, facilitatedTxId)));
    }
}
