package piuk.blockchain.android.injection;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;
import piuk.blockchain.android.data.services.ExchangeService;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.account.AccountEditViewModel;
import piuk.blockchain.android.ui.account.AccountViewModel;
import piuk.blockchain.android.ui.auth.PasswordRequiredViewModel;
import piuk.blockchain.android.ui.auth.PinEntryViewModel;
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter;
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment;
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferPresenter;
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.buy.BuyViewModel;
import piuk.blockchain.android.ui.chooser.AccountChooserViewModel;
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailViewModel;
import piuk.blockchain.android.ui.contacts.list.ContactsListViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactPairingMethodViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsInvitationBuilderViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsQrViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialogViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactsPaymentRequestViewModel;
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialogViewModel;
import piuk.blockchain.android.ui.home.MainViewModel;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.ui.login.LoginActivity;
import piuk.blockchain.android.ui.login.ManualPairingViewModel;
import piuk.blockchain.android.ui.onboarding.OnboardingViewModel;
import piuk.blockchain.android.ui.receive.ReceiveQrViewModel;
import piuk.blockchain.android.ui.receive.ReceiveViewModel;
import piuk.blockchain.android.ui.recover.RecoverFundsViewModel;
import piuk.blockchain.android.ui.send.SendViewModel;
import piuk.blockchain.android.ui.settings.SettingsViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveFragment;
import piuk.blockchain.android.ui.transactions.TransactionDetailViewModel;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity;

/**
 * Subcomponents have access to all upstream objects in the graph but can have their own scope -
 * they don't need to explicitly state their dependencies as they have access anyway
 */
@ViewModelScope
@Subcomponent(modules = DataManagerModule.class)
public interface DataManagerComponent {

    @Deprecated void inject(PasswordRequiredViewModel passwordRequiredViewModel);

    @Deprecated void inject(ManualPairingViewModel manualPairingViewModel);

    @Deprecated void inject(SendViewModel sendViewModel);

    @Deprecated void inject(PinEntryViewModel pinEntryViewModel);

    @Deprecated void inject(MainViewModel mainViewModel);

    @Deprecated void inject(AccountEditViewModel accountEditViewModel);

    @Deprecated void inject(RecoverFundsViewModel recoverFundsViewModel);

    @Deprecated void inject(ReceiveViewModel receiveViewModel);

    @Deprecated void inject(TransactionDetailViewModel transactionDetailViewModel);

    @Deprecated void inject(ConfirmFundsTransferPresenter confirmFundsTransferViewModel);

    @Deprecated void inject(AccountViewModel accountViewModel);

    @Deprecated void inject(SettingsViewModel settingsViewModel);

    @Deprecated void inject(FingerprintDialogViewModel fingerprintDialogViewModel);

    @Deprecated void inject(ReceiveQrViewModel receiveQrViewModel);

    @Deprecated void inject(ContactsListViewModel contactsListViewModel);

    @Deprecated void inject(ContactPairingMethodViewModel contactPairingMethodViewModel);

    @Deprecated void inject(ContactsInvitationBuilderViewModel contactsInvitationBuilderViewModel);

    @Deprecated void inject(ContactsQrViewModel contactsQrViewModel);

    @Deprecated void inject(ContactDetailViewModel contactDetailViewModel);

    @Deprecated void inject(ContactsPaymentRequestViewModel contactsPaymentRequestViewModel);

    @Deprecated void inject(AccountChooserViewModel accountChooserViewModel);

    @Deprecated void inject(BackupWalletCompletedPresenter backupWalletCompletedPresenter);

    @Deprecated void inject(WebSocketService webSocketService);

    @Deprecated void inject(ContactPaymentDialogViewModel contactPaymentDialogViewModel);

    @Deprecated void inject(OnboardingViewModel onboardingViewModel);

    @Deprecated void inject(BuyViewModel buyViewModel);

    @Deprecated void inject(ExchangeService exchangeService);

    // Activity/Fragment injection
    void inject(@NotNull LauncherActivity launcherActivity);

    void inject(@NotNull LoginActivity loginActivity);

    void inject(@NotNull SwipeToReceiveFragment swipeToReceiveFragment);

    void inject(@NotNull UpgradeWalletActivity upgradeWalletActivity);

    void inject(@NotNull BalanceFragment balanceFragment);

    void inject(@NotNull CreateWalletActivity createWalletActivity);

    void inject(@NotNull BackupWalletStartingFragment backupWalletStartingFragment);

    void inject(@NotNull BackupWalletWordListFragment backupWalletWordListFragment);

    void inject(@NotNull ConfirmPaymentDialog confirmPaymentDialog);
}
