package piuk.blockchain.android.ui.swipetoreceive;

import info.blockchain.api.data.Balance;
import info.blockchain.wallet.payload.data.Account;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.util.PrefsUtil;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ACCOUNT_NAME;
import static piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ADDRESSES;

public class SwipeToReceiveHelperTest extends RxTest {

    private SwipeToReceiveHelper subject;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private PrefsUtil prefsUtil;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SwipeToReceiveHelper(payloadDataManager, prefsUtil);
    }

    @Test
    public void updateAndStoreAddresses() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)).thenReturn(true);
        Account mockAccount = mock(Account.class);
        when(payloadDataManager.getDefaultAccount()).thenReturn(mockAccount);
        when(mockAccount.getLabel()).thenReturn("Account");
        when(payloadDataManager.getReceiveAddressAtPosition(eq(mockAccount), anyInt())).thenReturn("address");
        // Act
        subject.updateAndStoreAddresses();
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true);
        verify(payloadDataManager, times(5)).getReceiveAddressAtPosition(eq(mockAccount), anyInt());
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "Account");
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ADDRESSES, "address,address,address,address,address,");
    }

    @Test
    public void getNextAvailableAddressValid() throws Exception {
        // Arrange
        LinkedHashMap<String, Balance> map = new LinkedHashMap<>();
        Balance balance0 = new Balance();
        Balance balance1 = new Balance();
        Balance balance2 = new Balance();
        Balance balance3 = new Balance();
        Balance balance4 = new Balance();
        balance0.setFinalBalance(BigInteger.valueOf(1000L));
        balance1.setFinalBalance(BigInteger.valueOf(5L));
        balance2.setFinalBalance(BigInteger.valueOf(-10L));
        balance3.setFinalBalance(BigInteger.valueOf(0L));
        balance4.setFinalBalance(BigInteger.valueOf(0L));
        map.put("addr0", balance0);
        map.put("addr1", balance1);
        map.put("addr2", balance2);
        map.put("addr3", balance3);
        map.put("addr4", balance4);
        when(payloadDataManager.getBalanceOfAddresses(anyList())).thenReturn(Observable.just(map));
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        TestObserver<String> testObserver = subject.getNextAvailableAddress().test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue("addr3");
    }

    @Test
    public void getNextAvailableAddressAllUsed() throws Exception {
        // Arrange
        LinkedHashMap<String, Balance> map = new LinkedHashMap<>();
        Balance balance0 = new Balance();
        Balance balance1 = new Balance();
        Balance balance2 = new Balance();
        Balance balance3 = new Balance();
        Balance balance4 = new Balance();
        balance0.setFinalBalance(BigInteger.valueOf(1000L));
        balance1.setFinalBalance(BigInteger.valueOf(5L));
        balance2.setFinalBalance(BigInteger.valueOf(-10L));
        balance3.setFinalBalance(BigInteger.valueOf(1L));
        balance4.setFinalBalance(BigInteger.valueOf(1_000_000_000_000L));
        map.put("addr0", balance0);
        map.put("addr1", balance1);
        map.put("addr2", balance2);
        map.put("addr3", balance3);
        map.put("addr4", balance4);
        when(payloadDataManager.getBalanceOfAddresses(anyList())).thenReturn(Observable.just(map));
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        TestObserver<String> testObserver = subject.getNextAvailableAddress().test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue("");
    }

    @Test
    public void getReceiveAddresses() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        List<String> result = subject.getReceiveAddresses();
        // Assert
        assertEquals(5, result.size());
    }

    @Test
    public void getReceiveAddressesEmptyList() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("");
        // Act
        List<String> result = subject.getReceiveAddresses();
        // Assert
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void getAccountName() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "")).thenReturn("Account");
        // Act
        String result = subject.getAccountName();
        // Assert
        assertEquals("Account", result);
    }

}