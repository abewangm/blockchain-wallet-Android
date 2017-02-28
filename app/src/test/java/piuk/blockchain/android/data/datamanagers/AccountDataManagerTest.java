package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;

import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payload.data.Wallet;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.AddressInfoService;
import piuk.blockchain.android.data.services.BlockExplorerService;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountDataManagerTest extends RxTest {

    private AccountDataManager subject;
    @Mock PayloadManager payloadManager;
    @Mock MultiAddrFactory multiAddrFactory;
    @Mock AddressInfoService addressInfoService;
    @Mock BlockExplorerService blockExplorerService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountDataManager(payloadManager, multiAddrFactory, addressInfoService);
    }

    @Test
    public void createNewAccountSuccess() throws Exception {
        // Arrange
        Account mockAccount = mock(Account.class);
        when(payloadManager.addAccount(anyString(), isNull())).thenReturn(mockAccount);
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", null).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockAccount, observer.values().get(0));
    }

    @Test
    public void createNewAccountDecryptionFailure() throws Exception {
        // Arrange
        when(payloadManager.addAccount(anyString(), anyString())).thenThrow(new DecryptionException());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", "password").test();
        // Assert
        observer.assertError(DecryptionException.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

    @Test
    public void createNewAccountFatalException() throws Exception {
        // Arrange
        when(payloadManager.addAccount(anyString(), anyString())).thenThrow(new Exception());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", "password").test();
        // Assert
        observer.assertError(Exception.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

    @Test
    public void createNewAccountThrowsException() throws Exception {
        // Arrange
        doThrow(new Exception())
                .when(payloadManager).addAccount(
                anyString(), anyString());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", "password").test();
        // Assert
        observer.assertError(Exception.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

//    @Test
//    public void setPrivateKeySuccessNoDoubleEncryption() throws Exception {
    // TODO: 28/02/2017
//        // Arrange
//        ECKey mockECKey = mock(ECKey.class);
//        when(payloadManager.setKeyForLegacyAddress(any(ECKey.class), isNull())).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.setPrivateKey(mockECKey, null).test();
//        // Assert
//        observer.assertNoErrors();
//        observer.assertComplete();
//        assertEquals(true, observer.values().get(0).booleanValue());
//    }
//
//    @Test
//    public void setPrivateKeySuccessWithDoubleEncryption() throws Exception {
    // TODO: 28/02/2017
//        // Arrange
//        ECKey mockECKey = mock(ECKey.class);
//        when(payloadManager.setKeyForLegacyAddress(any(ECKey.class), any(CharSequenceX.class))).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.setPrivateKey(mockECKey, new CharSequenceX("password")).test();
//        // Assert
//        observer.assertNoErrors();
//        observer.assertComplete();
//        assertEquals(true, observer.values().get(0).booleanValue());
//    }

    // TODO: 28/02/2017 adding legacy address changed slightly
//    @Test
//    public void updateLegacyAddressSuccess() throws Exception {
//        // Arrange
//        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
//        Wallet mockPayload = mock(Wallet.class);
//        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
//        when(payloadManager.getPayload()).thenReturn(mockPayload);
//        when(payloadManager.addLegacyAddress(anyString(), anyString())).thenReturn(true);
//        when(addressInfoService.getAddressBalance(any(LegacyAddress.class), anyString())).thenReturn(Observable.just(0L));
//        // Act
//        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
//        // Assert
//        observer.assertNoErrors();
//        observer.assertComplete();
//        assertEquals(true, observer.values().get(0).booleanValue());
//    }
//
//    @Test
//    public void updateLegacyAddressFailure() throws Exception {
//        // Arrange
//        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
//        Wallet mockPayload = mock(Wallet.class);
//        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
//        when(payloadManager.getPayload()).thenReturn(mockPayload);
//        when(payloadManager.addLegacyAddress(anyString(), anyString())).thenReturn(false);
//        // Act
//        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
//        // Assert
//        observer.assertNoErrors();
//        observer.assertComplete();
//        assertEquals(false, observer.values().get(0).booleanValue());
//    }
//
//    @Test
//    public void updateLegacyAddressSuccessThrowsException() throws Exception {
//        // Arrange
//        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
//        Wallet mockPayload = mock(Wallet.class);
//        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
//        when(payloadManager.addLegacyAddress(anyString(), anyString())).thenReturn(true);
//        // Act
//        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
//        // Assert
//        observer.assertError(Throwable.class);
//        observer.assertNotComplete();
//        observer.assertNoValues();
//    }

}