package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

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
    @Mock PayloadManager payloadManager;
    @Mock PrivateKeyFactory privateKeyFactory;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountDataManager(payloadManager, privateKeyFactory);
    }

    @Test
    public void createNewAccount() throws Exception {
        // Arrange
        Account mockAccount = mock(Account.class);
        when(payloadManager.addAccount(anyString(), isNull())).thenReturn(mockAccount);
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", null).test();
        // Assert
        verify(payloadManager).addAccount("", null);
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockAccount, observer.values().get(0));
    }

    @Test
    public void setPrivateKeySuccessNoDoubleEncryption() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        when(payloadManager.setKeyForLegacyAddress(eq(mockECKey), isNull())).thenReturn(mockLegacyAddress);
        // Act
        TestObserver<LegacyAddress> observer = subject.setPrivateKey(mockECKey, null).test();
        // Assert
        verify(payloadManager).setKeyForLegacyAddress(eq(mockECKey), isNull());
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockLegacyAddress, observer.values().get(0));
    }

    @Test
    public void setPrivateKey() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        String password = "PASSWORD";
        when(payloadManager.setKeyForLegacyAddress(mockECKey, password)).thenReturn(mockLegacyAddress);
        // Act
        TestObserver<LegacyAddress> observer = subject.setPrivateKey(mockECKey, password).test();
        // Assert
        verify(payloadManager).setKeyForLegacyAddress(mockECKey, password);
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
        when(payloadManager.setKeyForLegacyAddress(mockECKey, password)).thenReturn(mockLegacyAddress);
        // Act
        TestObserver<LegacyAddress> observer = subject.setKeyForLegacyAddress(mockECKey, password).test();
        // Assert
        verify(payloadManager).setKeyForLegacyAddress(mockECKey, password);
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockLegacyAddress, observer.values().get(0));
    }

    @Test
    public void updateLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        // Act
        TestObserver<Void> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
        // Assert
        verify(payloadManager).addLegacyAddress(mockLegacyAddress);
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

    @Test
    public void updateMultiAddress() throws Exception {
        // Arrange

        // Act
        TestObserver<Void> testObserver = subject.updateMultiAddress().test();
        // Assert
        verify(payloadManager).updateAllTransactions(50, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void save() throws Exception {
        // Arrange

        // Act
        TestObserver<Void> testObserver = subject.save().test();
        // Assert
        verify(payloadManager).save();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

}