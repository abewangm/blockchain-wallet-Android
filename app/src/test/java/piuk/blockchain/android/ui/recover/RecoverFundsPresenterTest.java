package piuk.blockchain.android.ui.recover;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RecoverFundsPresenterTest {

    private RecoverFundsPresenter mSubject;

    @Mock private RecoverFundsActivity mActivity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mSubject = new RecoverFundsPresenter();
        mSubject.initView(mActivity);
    }

    /**
     * Recovery phrase missing, should inform user.
     */
    @Test
    public void onContinueClickedNoRecoveryPhrase() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn(null);
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Recovery phrase is too short to be valid, should inform user.
     */
    @Test
    public void onContinueClickedInvalidRecoveryPhraseLength() throws Exception {
        // Arrange
        when(mActivity.getRecoveryPhrase()).thenReturn("one two three four");
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Successful restore. Should take the user to the PIN entry page.
     */
    @Test
    public void onContinueClickedSuccessful() throws Exception {
        // Arrange
        String mnemonic = "all all all all all all all all all all all all";
        when(mActivity.getRecoveryPhrase()).thenReturn(mnemonic);
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mActivity).getRecoveryPhrase();
        verify(mActivity).gotoCredentialsActivity(mnemonic);
        verifyNoMoreInteractions(mActivity);
    }

    /**
     * Restore failed, inform the user.
     */
    @Test
    public void onContinueClickedFailed() throws Exception {
        // Arrange
        // TODO: 13/07/2017 isValidMnemonic not testable
        // Act

        // Assert
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

}