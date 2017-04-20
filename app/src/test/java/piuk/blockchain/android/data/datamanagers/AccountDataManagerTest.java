package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.services.PayloadService;

import static info.blockchain.wallet.util.PrivateKeyFactory.BASE58;
import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AccountDataManagerTest extends RxTest {

    private AccountDataManager subject;
    @Mock private PayloadService payloadService;
    @Mock private PrivateKeyFactory privateKeyFactory;
    @Mock private RxBus rxBus;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountDataManager(payloadService, privateKeyFactory, rxBus);
    }

    @Test
    public void createNewAccount() throws Exception {
        // Arrange
        Account mockAccount = mock(Account.class);
        when(payloadService.createNewAccount(anyString(), isNull()))
                .thenReturn(Observable.just(mockAccount));
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", null).test();
        // Assert
        verify(payloadService).createNewAccount("", null);
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockAccount, observer.values().get(0));
    }

    @Test
    public void setPrivateKeySuccessNoDoubleEncryption() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        when(payloadService.setKeyForLegacyAddress(eq(mockECKey), isNull()))
                .thenReturn(Observable.just(mockLegacyAddress));
        // Act
        TestObserver<LegacyAddress> observer = subject.setKeyForLegacyAddress(mockECKey, null).test();
        // Assert
        verify(payloadService).setKeyForLegacyAddress(eq(mockECKey), isNull());
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockLegacyAddress, observer.values().get(0));
    }

    @Test
    public void setKeyForLegacyAddress() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        String password = "PASSWORD";
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        when(payloadService.setKeyForLegacyAddress(mockECKey, password))
                .thenReturn(Observable.just(mockLegacyAddress));
        // Act
        TestObserver<LegacyAddress> observer = subject.setKeyForLegacyAddress(mockECKey, password).test();
        // Assert
        verify(payloadService).setKeyForLegacyAddress(mockECKey, password);
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockLegacyAddress, observer.values().get(0));
    }

    @Test
    public void addLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        when(payloadService.addLegacyAddress(mockLegacyAddress)).thenReturn(Completable.complete());
        // Act
        TestObserver<Void> observer = subject.addLegacyAddress(mockLegacyAddress).test();
        // Assert
        verify(payloadService).addLegacyAddress(mockLegacyAddress);
        observer.assertNoErrors();
        observer.assertComplete();
    }

    @Test
    public void updateLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        when(payloadService.updateLegacyAddress(mockLegacyAddress)).thenReturn(Completable.complete());
        // Act
        TestObserver<Void> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
        // Assert
        verify(payloadService).updateLegacyAddress(mockLegacyAddress);
        observer.assertNoErrors();
        observer.assertComplete();
    }

    @Test
    public void getKeyFromImportedData() throws Exception {
        // Arrange
        String data = "DATA";
        ECKey mockEcKey = mock(ECKey.class);
        when(privateKeyFactory.getKey(BASE58, data)).thenReturn(mockEcKey);
        // Act
        TestObserver<ECKey> testObserver = subject.getKeyFromImportedData(BASE58, data).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(mockEcKey);
    }

}