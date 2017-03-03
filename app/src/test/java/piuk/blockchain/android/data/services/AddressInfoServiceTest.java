package piuk.blockchain.android.data.services;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.api.data.Address;

import info.blockchain.wallet.payload.data.LegacyAddress;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressInfoServiceTest extends RxTest {

    private BlockExplorerService subject;
    @Mock
    BlockExplorer blockExplorer;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new BlockExplorerService(new BlockExplorer());
    }

    @Test
    public void getAddressBalanceSuccess() throws Exception {
        // Arrange
        LegacyAddress mockAddress = mock(LegacyAddress.class);
        when(mockAddress.getAddress()).thenReturn("1234567890");
        when(blockExplorer.getAddress(anyString(), anyInt(), anyInt()).execute().body()).thenReturn(getValidAddress());
        // Act
        TestObserver<Address> observer = subject.getAddress(mockAddress.getAddress(), 0, 0).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        Assert.assertEquals(1000L, observer.values().get(0).getFinalBalance());
    }

    private Address getValidAddress() throws IOException {
        return Address.fromJson("{\n" +
            "  \"final_balance\": 1000\n" +
            "}");
    }
}