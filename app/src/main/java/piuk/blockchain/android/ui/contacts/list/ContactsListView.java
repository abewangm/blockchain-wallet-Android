package piuk.blockchain.android.ui.contacts.list;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.List;

import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.base.View;
import piuk.blockchain.android.ui.customviews.ToastCustom;

interface ContactsListView extends View {

    Intent getPageIntent();

    void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

    void setUiState(@UiState.UiStateDef int uiState);

    void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    void showProgressDialog();

    void dismissProgressDialog();

    void showSecondPasswordDialog();

    void onLinkGenerated(Intent intent);
}
