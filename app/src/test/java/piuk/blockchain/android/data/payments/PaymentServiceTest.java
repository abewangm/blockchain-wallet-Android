package piuk.blockchain.android.data.payments;

import info.blockchain.api.data.UnspentOutput;
import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.exceptions.ApiException;
import info.blockchain.wallet.payment.InsufficientMoneyException;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.observers.TestObserver;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.RxTest;
import retrofit2.Call;
import retrofit2.Response;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class PaymentServiceTest extends RxTest {

    private PaymentService subject;
    @Mock private Payment payment;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new PaymentService(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void submitPaymentSuccess() throws Exception {
        // Arrange
        String txHash = "TX_HASH";
        SpendableUnspentOutputs mockOutputBundle = mock(SpendableUnspentOutputs.class);
        List<UnspentOutput> mockOutputs = Collections.singletonList(mock(UnspentOutput.class));
        when(mockOutputBundle.getSpendableOutputs()).thenReturn(mockOutputs);
        List<ECKey> mockEcKeys = Collections.singletonList(mock(ECKey.class));
        String toAddress = "TO_ADDRESS";
        String changeAddress = "CHANGE_ADDRESS";
        BigInteger mockFee = mock(BigInteger.class);
        BigInteger mockAmount = mock(BigInteger.class);
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getHashAsString()).thenReturn(txHash);
        when(payment.makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress)))
                .thenReturn(mockTx);
        Call<ResponseBody> mockCall = mock(Call.class);
        Response response = Response.success(mock(ResponseBody.class));
        when(mockCall.execute()).thenReturn(response);
        when(payment.publishTransaction(mockTx)).thenReturn(mockCall);
        // Act
        TestObserver<String> testObserver = subject.submitPayment(mockOutputBundle,
                mockEcKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(txHash, testObserver.values().get(0));
        verify(payment).makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress));
        verify(payment).signTransaction(mockTx, mockEcKeys);
        verify(payment).publishTransaction(mockTx);
        verifyNoMoreInteractions(payment);
    }

    @Test
    public void submitPaymentFailure() throws Exception {
        // Arrange
        String txHash = "TX_HASH";
        SpendableUnspentOutputs mockOutputBundle = mock(SpendableUnspentOutputs.class);
        List<UnspentOutput> mockOutputs = Collections.singletonList(mock(UnspentOutput.class));
        when(mockOutputBundle.getSpendableOutputs()).thenReturn(mockOutputs);
        List<ECKey> mockEcKeys = Collections.singletonList(mock(ECKey.class));
        String toAddress = "TO_ADDRESS";
        String changeAddress = "CHANGE_ADDRESS";
        BigInteger mockFee = mock(BigInteger.class);
        BigInteger mockAmount = mock(BigInteger.class);
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getHashAsString()).thenReturn(txHash);
        when(payment.makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress)))
                .thenReturn(mockTx);
        Call<ResponseBody> mockCall = mock(Call.class);
        Response response = Response.error(500, ResponseBody.create(MediaType.parse("application/json"), "{}"));
        when(mockCall.execute()).thenReturn(response);
        when(payment.publishTransaction(mockTx)).thenReturn(mockCall);
        // Act
        TestObserver<String> testObserver = subject.submitPayment(mockOutputBundle,
                mockEcKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount).test();
        // Assert
        testObserver.assertNotComplete();
        testObserver.assertTerminated();
        testObserver.assertNoValues();
        testObserver.assertError(Throwable.class);
        verify(payment).makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress));
        verify(payment).signTransaction(mockTx, mockEcKeys);
        verify(payment).publishTransaction(mockTx);
        verifyNoMoreInteractions(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void submitPaymentException() throws Exception {
        // Arrange
        String txHash = "TX_HASH";
        SpendableUnspentOutputs mockOutputBundle = mock(SpendableUnspentOutputs.class);
        List<UnspentOutput> mockOutputs = Collections.singletonList(mock(UnspentOutput.class));
        when(mockOutputBundle.getSpendableOutputs()).thenReturn(mockOutputs);
        List<ECKey> mockEcKeys = Collections.singletonList(mock(ECKey.class));
        String toAddress = "TO_ADDRESS";
        String changeAddress = "CHANGE_ADDRESS";
        BigInteger mockFee = mock(BigInteger.class);
        BigInteger mockAmount = mock(BigInteger.class);
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getHashAsString()).thenReturn(txHash);
        when(payment.makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress)))
                .thenThrow(new InsufficientMoneyException(new BigInteger("1")));
        // Act
        TestObserver<String> testObserver = subject.submitPayment(mockOutputBundle,
                mockEcKeys,
                toAddress,
                changeAddress,
                mockFee,
                mockAmount).test();
        // Assert
        testObserver.assertNotComplete();
        testObserver.assertTerminated();
        testObserver.assertNoValues();
        testObserver.assertError(InsufficientMoneyException.class);
        verify(payment).makeTransaction(eq(mockOutputs), any(HashMap.class), eq(mockFee), eq(changeAddress));
        verifyNoMoreInteractions(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUnspentOutputsSuccess() throws Exception {
        // Arrange
        String address = "ADDRESS";
        Call<UnspentOutputs> mockCall = mock(Call.class);
        UnspentOutputs mockOutputs = mock(UnspentOutputs.class);
        Response<UnspentOutputs> response = Response.success(mockOutputs);
        when(mockCall.execute()).thenReturn(response);
        when(payment.getUnspentCoins(Collections.singletonList(address))).thenReturn(mockCall);
        // Act
        TestObserver<UnspentOutputs> testObserver = subject.getUnspentOutputs(address).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(mockOutputs, testObserver.values().get(0));
        verify(payment).getUnspentCoins(Collections.singletonList(address));
        verifyNoMoreInteractions(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUnspentOutputs500Error() throws Exception {
        // Arrange
        String address = "ADDRESS";
        Call<UnspentOutputs> mockCall = mock(Call.class);
        Response<UnspentOutputs> response = Response.error(500, mock(ResponseBody.class));
        when(mockCall.execute()).thenReturn(response);
        when(payment.getUnspentCoins(Collections.singletonList(address))).thenReturn(mockCall);
        // Act
        TestObserver<UnspentOutputs> testObserver = subject.getUnspentOutputs(address).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertTrue(testObserver.values().get(0).getUnspentOutputs().isEmpty());
        verify(payment).getUnspentCoins(Collections.singletonList(address));
        verifyNoMoreInteractions(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUnspentOutputsFailed() throws Exception {
        // Arrange
        String address = "ADDRESS";
        Call<UnspentOutputs> mockCall = mock(Call.class);
        Response<UnspentOutputs> response = Response.error(404, mock(ResponseBody.class));
        when(mockCall.execute()).thenReturn(response);
        when(payment.getUnspentCoins(Collections.singletonList(address))).thenReturn(mockCall);
        // Act
        TestObserver<UnspentOutputs> testObserver = subject.getUnspentOutputs(address).test();
        // Assert
        testObserver.assertNotComplete();
        testObserver.assertTerminated();
        testObserver.assertNoValues();
        testObserver.assertError(ApiException.class);
        verify(payment).getUnspentCoins(Collections.singletonList(address));
        verifyNoMoreInteractions(payment);
    }

    @Test
    public void getSpendableCoins() throws Exception {
        // Arrange
        UnspentOutputs mockUnspent = mock(UnspentOutputs.class);
        BigInteger mockPayment = mock(BigInteger.class);
        BigInteger mockFee = mock(BigInteger.class);
        SpendableUnspentOutputs mockOutputs = mock(SpendableUnspentOutputs.class);
        when(payment.getSpendableCoins(mockUnspent, mockPayment, mockFee)).thenReturn(mockOutputs);
        // Act
        SpendableUnspentOutputs result = subject.getSpendableCoins(mockUnspent, mockPayment, mockFee);
        // Assert
        assertEquals(mockOutputs, result);
        verify(payment).getSpendableCoins(mockUnspent, mockPayment, mockFee);
        verifyNoMoreInteractions(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getSweepableCoins() throws Exception {
        // Arrange
        UnspentOutputs mockUnspent = mock(UnspentOutputs.class);
        BigInteger mockFee = mock(BigInteger.class);
        Pair<BigInteger, BigInteger> mockSweepableCoins = mock(Pair.class);
        when(payment.getSweepableCoins(mockUnspent, mockFee)).thenReturn(mockSweepableCoins);
        // Act
        Pair<BigInteger, BigInteger> result = subject.getSweepableCoins(mockUnspent, mockFee);
        // Assert
        assertEquals(mockSweepableCoins, result);
        verify(payment).getSweepableCoins(mockUnspent, mockFee);
        verifyNoMoreInteractions(payment);
    }

    @Test
    public void isAdequateFee() throws Exception {
        // Arrange
        int inputs = 1;
        int outputs = 101;
        BigInteger mockFee = mock(BigInteger.class);
        when(payment.isAdequateFee(inputs, outputs, mockFee)).thenReturn(false);
        // Act
        boolean result = subject.isAdequateFee(inputs, outputs, mockFee);
        // Assert
        assertEquals(false, result);
        verify(payment).isAdequateFee(inputs, outputs, mockFee);
        verifyNoMoreInteractions(payment);
    }

    @Test
    public void estimateSize() throws Exception {
        // Arrange
        int inputs = 1;
        int outputs = 101;
        int estimatedSize = 1337;
        when(payment.estimatedSize(inputs, outputs)).thenReturn(estimatedSize);
        // Act
        int result = subject.estimateSize(inputs, outputs);
        // Assert
        assertEquals(estimatedSize, result);
        verify(payment).estimatedSize(inputs, outputs);
        verifyNoMoreInteractions(payment);
    }

    @Test
    public void estimateFee() throws Exception {
        // Arrange
        int inputs = 1;
        int outputs = 101;
        BigInteger mockFeePerKb = mock(BigInteger.class);
        BigInteger mockAbsoluteFee = mock(BigInteger.class);
        when(payment.estimatedFee(inputs, outputs, mockFeePerKb)).thenReturn(mockAbsoluteFee);
        // Act
        BigInteger result = subject.estimateFee(inputs, outputs, mockFeePerKb);
        // Assert
        assertEquals(mockAbsoluteFee, result);
        verify(payment).estimatedFee(inputs, outputs, mockFeePerKb);
        verifyNoMoreInteractions(payment);
    }
}