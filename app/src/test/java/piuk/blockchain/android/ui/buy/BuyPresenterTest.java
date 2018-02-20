package piuk.blockchain.android.ui.buy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.exchange.BuyDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.exchange.models.WebViewLoginDetails;
import piuk.blockchain.android.data.walletoptions.WalletOptionsDataManager;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.util.AppUtil;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuyPresenterTest {

    private BuyPresenter subject;

    @Mock private BuyView activity;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private BuyDataManager buyDataManager;
    @Mock private AppUtil appUtil;
    @Mock private EnvironmentSettings environmentSettings;
    @Mock private WalletOptionsDataManager walletOptionsDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new BuyPresenter(appUtil, buyDataManager, payloadDataManager, environmentSettings, walletOptionsDataManager);
        subject.initView(activity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange
        WebViewLoginDetails webViewLoginDetails = new WebViewLoginDetails("", "",
                "", "");
        when(payloadDataManager.loadNodes()).thenReturn(
                Observable.just(true));
        when(buyDataManager.getWebViewLoginDetails()).thenReturn(
                Observable.just(webViewLoginDetails));

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).setWebViewLoginDetails(webViewLoginDetails);
    }

    @Test
    public void onViewReady_secondPassword() throws Exception {
        // Arrange
        WebViewLoginDetails webViewLoginDetails = new WebViewLoginDetails("", "",
                "", "");
        when(payloadDataManager.loadNodes()).thenReturn(
                Observable.just(false));
        when(buyDataManager.getWebViewLoginDetails()).thenReturn(
                Observable.just(webViewLoginDetails));
        when(payloadDataManager.isDoubleEncrypted()).thenReturn(
                false);

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showSecondPasswordDialog();
        verify(activity).setUiState(UiState.EMPTY);
    }

}
