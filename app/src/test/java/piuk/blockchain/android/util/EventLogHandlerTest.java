package piuk.blockchain.android.util;

import info.blockchain.wallet.util.WebUtil;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by riaanvos on 17/10/2016.
 */

public class EventLogHandlerTest {

    @Mock PrefsUtil prefsUtil;
    @Mock WebUtil webUtil;

    private static final String SUCCESS_TRUE = "{\"success\":true}";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void arrangeShouldLog() throws Exception{
        when(webUtil.getURL(anyString())).thenReturn(SUCCESS_TRUE);
        when(prefsUtil.getValue(anyString(), anyBoolean())).thenReturn(false);//has not been logged
    }

    private void arrangeShouldNotLog() throws Exception{
        when(webUtil.getURL(anyString())).thenReturn(SUCCESS_TRUE);
        when(prefsUtil.getValue(anyString(), anyBoolean())).thenReturn(true);//has been logged
    }

    @Test
    public void enabled_log2ndPw_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.log2ndPwEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_2ND_PW + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Test
    public void enabled_log2ndPw_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.log2ndPwEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_2ND_PW + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logLegacy_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logLegacyEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_LEGACY + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_LEGACY, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logLegacy_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logLegacyEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_LEGACY + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_LEGACY, false);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logBackup_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logBackupEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_BACKUP + "1");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_BACKUP, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void enabled_logBackup_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logBackupEvent(true);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_BACKUP + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_BACKUP, false);
    }


    @Test
    public void disabled_log2ndPw_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.log2ndPwEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_2ND_PW + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Test
    public void disabled_log2ndPw_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.log2ndPwEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_2ND_PW + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_2ND_PW, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logLegacy_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logLegacyEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_LEGACY + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_LEGACY, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logLegacy_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logLegacyEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_LEGACY + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_LEGACY, false);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logBackup_notPreviouslyLogged_shouldLog() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logBackupEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_BACKUP + "0");
        verify(prefsUtil).setValue(PrefsUtil.KEY_EVENT_BACKUP, true);
    }

    @Ignore //20/12/2016 Disabled
    @Test
    public void disabled_logBackup_previouslyLogged_shouldNotLog() throws Exception {
        // Arrange
        arrangeShouldNotLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logBackupEvent(false);

        // Assert
        Thread.sleep(200);
        verify(webUtil, never()).getURL(EventLogHandler.URL_EVENT_BACKUP + "0");
        verify(prefsUtil, never()).setValue(PrefsUtil.KEY_EVENT_BACKUP, false);
    }

    @Ignore
    @Test
    public void logAddressInputEvent() throws Exception {
        // Arrange
        arrangeShouldLog();

        // Act
        EventLogHandler handler = new EventLogHandler(prefsUtil, webUtil);
        handler.logAddressInputEvent(EventLogHandler.URL_EVENT_TX_INPUT_FROM_DROPDOWN);

        // Assert
        Thread.sleep(200);
        verify(webUtil).getURL(EventLogHandler.URL_EVENT_TX_INPUT_FROM_DROPDOWN);
    }
}
