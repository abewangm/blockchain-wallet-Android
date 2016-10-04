package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.Nullable;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;
import info.blockchain.wallet.util.DoubleEncryptionFactory;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;

import java.util.List;

import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.AddressInfoService;
import rx.Observable;
import rx.exceptions.Exceptions;

import static piuk.blockchain.android.data.services.AddressInfoService.PARAMETER_FINAL_BALANCE;

public class AccountDataManager {

    private PayloadManager payloadManager;
    private MultiAddrFactory multiAddrFactory;
    private AddressInfoService addressInfoService;

    public AccountDataManager(PayloadManager payload, MultiAddrFactory addrFactory, AddressInfoService addressService) {
        payloadManager = payload;
        multiAddrFactory = addrFactory;
        addressInfoService = addressService;
    }

    public Observable<Account> createNewAccount(String accountLabel, @Nullable CharSequenceX secondPassword) {
        return createNewAccountObservable(accountLabel, secondPassword)
                .compose(RxUtil.applySchedulers());
    }

    public Observable<Boolean> setPrivateKey(ECKey key, @Nullable CharSequenceX secondPassword) {
        Payload payload = payloadManager.getPayload();
        int index = payload.getLegacyAddressStrings().indexOf(key.toAddress(MainNetParams.get()).toString());
        LegacyAddress legacyAddress = payload.getLegacyAddresses().get(index);
        if (!payload.isDoubleEncrypted()) {
            legacyAddress.setEncryptedKey(key.getPrivKeyBytes());
        } else {
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                    payload.getSharedKey(),
                    secondPassword != null ? secondPassword.toString() : null,
                    payload.getOptions().getIterations());
            legacyAddress.setEncryptedKey(encrypted2);
        }
        legacyAddress.setWatchOnly(false);
        payloadManager.setPayload(payload);
        return savePayloadToServer();
    }

    // TODO: 04/10/2016 This needs testing
    public Observable<Boolean> updateLegacyAddress(LegacyAddress address) {
        return Observable.fromCallable(() -> payloadManager.addLegacyAddress(address))
                .flatMap(success -> {
                    if (success) {
                        List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
                        try {
                            multiAddrFactory.refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
                        } catch (Exception e) {
                            throw Exceptions.propagate(e);
                        }

                        addAddressAndUpdate(address)
                                .flatMap(aLong -> refreshLegacyAddressData())
                                .doOnNext(aVoid -> Observable.just(true));
                    }

                    return Observable.just(false);
                });
    }

    private Observable<Boolean> savePayloadToServer() {
        return Observable.fromCallable(() -> payloadManager.savePayloadToServer())
                .compose(RxUtil.applySchedulers());
    }

    private Observable<Void> refreshLegacyAddressData() {
        return Observable.create(subscriber -> {
            try {
                List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStrings();
                multiAddrFactory.refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
                if (subscriber.isUnsubscribed()) return;
                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private Observable<Long> addAddressAndUpdate(LegacyAddress address) {
        return addressInfoService.getAddressBalance(address, PARAMETER_FINAL_BALANCE)
                .doOnNext(balance -> {
                    multiAddrFactory.setLegacyBalance(address.getAddress(), balance);
                    multiAddrFactory.setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() + balance);
                });
    }

    private Observable<Account> createNewAccountObservable(String accountLabel, @Nullable CharSequenceX secondPassword) {
        return Observable.create(subscriber -> {
            try {
                payloadManager.addAccount(
                        accountLabel,
                        secondPassword != null ? secondPassword.toString() : null,
                        new PayloadManager.AccountAddListener() {
                            @Override
                            public void onAccountAddSuccess(Account account) {
                                subscriber.onNext(account);
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onSecondPasswordFail() {
                                subscriber.onError(new DecryptionException());
                            }

                            @Override
                            public void onPayloadSaveFail() {
                                subscriber.onError(new PayloadException());
                            }
                        });
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
