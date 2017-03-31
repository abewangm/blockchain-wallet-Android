package piuk.blockchain.android.data.services;

import info.blockchain.wallet.api.data.Status;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import io.reactivex.Observable;
import piuk.blockchain.android.util.PrefsUtil;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by riaanvos on 17/10/2016.
 */

public class EventServiceTest {

    @Mock private PrefsUtil prefsUtil;
    @Mock private WalletService walletService;

    private static final String SUCCESS_TRUE = "{\"success\":true}";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void arrangeShouldLog() throws Exception {
        when(walletService.logEvent(anyString())).thenReturn(getSuccess());
        when(prefsUtil.getValue(anyString(), anyBoolean())).thenReturn(false);//has not been logged
    }

    private Observable<Status> getSuccess() throws IOException {
        return Observable.just(Status.fromJson(SUCCESS_TRUE));
    }

    private void arrangeShouldNotLog() throws Exception {
        when(walletService.logEvent(anyString())).thenReturn(getSuccess());
        when(prefsUtil.getValue(anyString(), anyBoolean())).thenReturn(true);//has been logged
    }

    @Test
    public void enabled_log2ndPw_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.log2ndPwEvent(true);

        // Assert
        verify(walletService).logEvent(EventService.EVENT_2ND_PW + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Test
    public void enabled_log2ndPw_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.log2ndPwEvent(true);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_2ND_PW + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logLegacy_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logLegacyEvent(true);

        // Assert
        Thread.sleep(200);
        verify(walletService).logEvent(EventService.EVENT_LEGACY + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_LEGACY, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logLegacy_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logLegacyEvent(true);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_LEGACY + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_LEGACY, false);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logBackup_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logBackupEvent(true);

        // Assert
        Thread.sleep(200);
        verify(walletService).logEvent(EventService.EVENT_BACKUP + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_BACKUP, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logBackup_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logBackupEvent(true);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_BACKUP + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_BACKUP, false);
    }

    @Test
    public void disabled_log2ndPw_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.log2ndPwEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService).logEvent(EventService.EVENT_2ND_PW + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Test
    public void disabled_log2ndPw_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.log2ndPwEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_2ND_PW + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logLegacy_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logLegacyEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService).logEvent(EventService.EVENT_LEGACY + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_LEGACY, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logLegacy_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logLegacyEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_LEGACY + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_LEGACY, false);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logBackup_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logBackupEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService).logEvent(EventService.EVENT_BACKUP + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_BACKUP, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logBackup_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logBackupEvent(false);

        // Assert
        Thread.sleep(200);
        verify(walletService, never()).logEvent(EventService.EVENT_BACKUP + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_BACKUP, false);
    }

    @Test
    public void logAddressInputEvent() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventService handler = new EventService(prefsUtil, walletService);
        handler.logAddressInputEvent(EventService.EVENT_TX_INPUT_FROM_DROPDOWN);

        // Assert
        verify(walletService).logEvent(EventService.EVENT_TX_INPUT_FROM_DROPDOWN);
    }
}
