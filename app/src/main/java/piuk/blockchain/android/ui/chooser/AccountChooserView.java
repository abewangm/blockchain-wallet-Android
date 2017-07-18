package piuk.blockchain.android.ui.chooser;

import java.util.List;

import piuk.blockchain.android.data.contacts.PaymentRequestType;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.base.View;

interface AccountChooserView extends View {

    PaymentRequestType getPaymentRequestType();

    void updateUi(List<ItemAccount> items);

    void showNoContacts();
}
