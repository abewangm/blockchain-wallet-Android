package piuk.blockchain.android.injection;

import dagger.Subcomponent;
import piuk.blockchain.android.data.services.ExchangeService;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.account.AccountEditViewModel;
import piuk.blockchain.android.ui.account.AccountViewModel;
import piuk.blockchain.android.ui.createwallet.CreateWalletPresenter;
import piuk.blockchain.android.ui.login.LoginPresenter;
import piuk.blockchain.android.ui.auth.PasswordRequiredViewModel;
import piuk.blockchain.android.ui.auth.PinEntryViewModel;
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedPresenter;
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingPresenter;
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferPresenter;
import piuk.blockchain.android.ui.backup.verify.BackupVerifyPresenter;
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListPresenter;
import piuk.blockchain.android.ui.balance.BalancePresenter;
import piuk.blockchain.android.ui.buy.BuyViewModel;
import piuk.blockchain.android.ui.chooser.AccountChooserViewModel;
import piuk.blockchain.android.ui.confirm.ConfirmPaymentPresenter;
import piuk.blockchain.android.ui.contacts.detail.ContactDetailViewModel;
import piuk.blockchain.android.ui.contacts.list.ContactsListViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactPairingMethodViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsInvitationBuilderViewModel;
import piuk.blockchain.android.ui.contacts.pairing.ContactsQrViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactPaymentDialogViewModel;
import piuk.blockchain.android.ui.contacts.payments.ContactsPaymentRequestViewModel;
import piuk.blockchain.android.ui.fingerprint.FingerprintDialogViewModel;
import piuk.blockchain.android.ui.home.MainViewModel;
import piuk.blockchain.android.ui.launcher.LauncherPresenter;
import piuk.blockchain.android.ui.onboarding.OnboardingViewModel;
import piuk.blockchain.android.ui.login.ManualPairingViewModel;
import piuk.blockchain.android.ui.pairing_code.PairingCodePresenter;
import piuk.blockchain.android.ui.receive.ReceiveQrViewModel;
import piuk.blockchain.android.ui.receive.ReceiveViewModel;
import piuk.blockchain.android.ui.recover.RecoverFundsViewModel;
import piuk.blockchain.android.ui.send.SendViewModel;
import piuk.blockchain.android.ui.settings.SettingsViewModel;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceivePresenter;
import piuk.blockchain.android.ui.transactions.TransactionDetailViewModel;
import piuk.blockchain.android.ui.upgrade.UpgradeWalletPresenter;

/**
 * Subcomponents have access to all upstream objects in the graph but can have their own scope -
 * they don't need to explicitly state their dependencies as they have access anyway
 */
@SuppressWarnings("WeakerAccess")
@ViewModelScope
@Subcomponent(modules = DataManagerModule.class)
public interface DataManagerComponent {

    void inject(LauncherPresenter launcherPresenter);

    void inject(PasswordRequiredViewModel passwordRequiredViewModel);

    void inject(ManualPairingViewModel manualPairingViewModel);

    void inject(SendViewModel sendViewModel);

    void inject(PinEntryViewModel pinEntryViewModel);

    void inject(MainViewModel mainViewModel);

    void inject(AccountEditViewModel accountEditViewModel);

    void inject(RecoverFundsViewModel recoverFundsViewModel);

    void inject(ReceiveViewModel receiveViewModel);

    void inject(TransactionDetailViewModel transactionDetailViewModel);

    void inject(ConfirmFundsTransferPresenter confirmFundsTransferViewModel);

    void inject(AccountViewModel accountViewModel);

    void inject(SettingsViewModel settingsViewModel);

    void inject(FingerprintDialogViewModel fingerprintDialogViewModel);

    void inject(ReceiveQrViewModel receiveQrViewModel);

    void inject(ContactsListViewModel contactsListViewModel);

    void inject(ContactPairingMethodViewModel contactPairingMethodViewModel);

    void inject(SwipeToReceivePresenter swipeToReceivePresenter);

    void inject(ContactsInvitationBuilderViewModel contactsInvitationBuilderViewModel);

    void inject(ContactsQrViewModel contactsQrViewModel);

    void inject(ContactDetailViewModel contactDetailViewModel);

    void inject(ContactsPaymentRequestViewModel contactsPaymentRequestViewModel);

    void inject(AccountChooserViewModel accountChooserViewModel);

    void inject(BackupWalletCompletedPresenter backupWalletViewModel);

    void inject(WebSocketService webSocketService);

    void inject(BackupVerifyPresenter backupVerifyViewModel);

    void inject(ContactPaymentDialogViewModel contactPaymentDialogViewModel);

    void inject(OnboardingViewModel onboardingViewModel);

    void inject(BuyViewModel buyViewModel);

    void inject(ExchangeService exchangeService);

    void inject(UpgradeWalletPresenter upgradeWalletViewModel);

    void inject(ConfirmPaymentPresenter confirmPaymentPresenter);

    void inject(PairingCodePresenter pairingCodePresenter);

    void inject(BalancePresenter balancePresenter);

    void inject(BackupWalletWordListPresenter backupWalletWordListPresenter);

    void inject(BackupWalletStartingPresenter backupWalletStartingPresenter);

    void inject(LoginPresenter loginPresenter);

    void inject(CreateWalletPresenter createWalletPresenter);
}
