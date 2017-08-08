package piuk.blockchain.android.ui.contacts.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.List;

import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactDetailView extends View {

    Bundle getPageBundle();

    void updateContactName(String name);

    void finishPage();

    void showRenameDialog(String name);

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void showProgressDialog();

    void dismissProgressDialog();

    void showDeleteUserDialog();

    void onTransactionsUpdated(List<Object> transactions, boolean isBtc);

    void showAccountChoiceDialog(List<String> accounts, String fctxId);

    void initiatePayment(String uri, String recipientId, String mdid, String fctxId);

    void showWaitingForPaymentDialog();

    void showWaitingForAddressDialog();

    void showTransactionDetail(String txHash);

    void showSendAddressDialog(String fctxId);

    void showTransactionDeclineDialog(String fctxId);

    void showTransactionCancelDialog(String fctxId);

    void showPayOrDeclineDialog(String fctxId, String balanceString, String name, @Nullable String note);
}
