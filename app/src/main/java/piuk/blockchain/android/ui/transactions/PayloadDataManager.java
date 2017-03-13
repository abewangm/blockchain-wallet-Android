package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.Wallet;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class PayloadDataManager {

    private PayloadManager payloadManager;

    public PayloadDataManager(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    ///////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a {@link DecryptionException} if the password isincorrect, otherwise can return a
     * {@link HDWalletException} which should be regarded as fatal.
     *
     * @param payload  The payload String to be decrypted
     * @param password The user's password
     * @return A {@link Completable} object
     */
    public Completable initializeFromPayload(String payload, String password) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecryptFromPayload(payload, password);
            return Void.TYPE;
        });
    }

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the {@link PayloadDataManager}.
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic   The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email      The user's email address, preferably not associated with another account
     * @param password   The user's choice of password
     * @return An {@link Observable<Wallet>}
     */
    public Observable<Wallet> restoreHdWallet(String mnemonic, String walletName, String email, String password) {
        return Observable.fromCallable(() ->
                payloadManager.recoverFromMnemonic(mnemonic, walletName, email, password));
    }

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password   The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email      The user's email address, preferably not associated with another account
     * @return An {@link Observable<Wallet>}
     */
    public Observable<Wallet> createHdWallet(String password, String walletName, String email) {
        return Observable.fromCallable(() -> payloadManager.create(walletName, email, password));
    }

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's  password.
     *
     * @param sharedKey The shared key as a String
     * @param guid      The user's GUID
     * @param password  The user's password
     * @return A {@link Completable} object
     */
    public Completable initializeAndDecrypt(String sharedKey, String guid, String password) {
        return Completable.fromCallable(() -> {
            payloadManager.initializeAndDecrypt(sharedKey, guid, password);
            return Void.TYPE;
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // THE NEXT LOGICAL BLOCK -> TODO: ORGANISE REMAINING METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Converts any address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as legacy address.
     * @return Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {
        return payloadManager.getLabelFromAddress(address);
    }

    /**
     * Returns a {@link Completable} which saves the current payload to the server.
     *
     * @return A {@link Completable} object
     */
    // TODO: 10/03/2017 Remove schedulers, let viewmodels handle it
    public Completable syncPayloadWithServer() {
        return Completable.fromCallable(() -> {
            payloadManager.save();
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    /**
     * Returns {@link Completable} which updates balances and transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A {@link Completable} object
     * @see IgnorableDefaultObserver
     */
    // TODO: 10/03/2017 Remove schedulers, let viewmodels handle it
    public Completable updateBalancesAndTransactions() {
        return Completable.fromCallable(() -> {
            payloadManager.getAllTransactions(50, 0);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Returns the next Receive address for a given account index.
     *
     * @param defaultIndex The index of the account for which you want an address to be generated
     * @return An {@link Observable} wrapping the receive address
     */
    public Observable<String> getNextReceiveAddress(int defaultIndex) {
        Account account = getWallet().getHdWallets().get(0).getAccounts().get(defaultIndex);
        return Observable.fromCallable(() -> payloadManager.getNextReceiveAddress(account));
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////

    public Wallet getWallet() {
        return payloadManager.getPayload();
    }

    public int getDefaultAccountIndex() {
        return getWallet().getHdWallets().get(0).getDefaultAccountIdx();
    }

}
