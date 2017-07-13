package piuk.blockchain.android.ui.recover;

import android.support.annotation.StringRes;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import info.blockchain.wallet.bip44.HDWalletFactory;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class RecoverFundsViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject AuthDataManager authDataManager;
    @Inject AppUtil appUtil;
    @Inject PrefsUtil prefsUtil;

    public interface DataListener {

        String getRecoveryPhrase();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog(@StringRes int messageId);

        void dismissProgressDialog();

        void gotoCredentialsActivity(String recoveryPhrase);

    }

    RecoverFundsViewModel(DataListener listener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        dataListener = listener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void onContinueClicked() {
        String recoveryPhrase = dataListener.getRecoveryPhrase();
        if (recoveryPhrase == null || recoveryPhrase.isEmpty()) {
            dataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            return;
        }

        try {
            if (isValidMnemonic(recoveryPhrase)) {
                dataListener.gotoCredentialsActivity(recoveryPhrase);
            } else {
                dataListener.showToast(R.string.invalid_recovery_phrase, ToastCustom.TYPE_ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            dataListener.showToast(R.string.restore_failed, ToastCustom.TYPE_ERROR);
        }
    }

    /**
     * We only support US english mnemonics atm
     * @throws MnemonicException.MnemonicWordException
     */
    private boolean isValidMnemonic(String recoveryPhrase) throws MnemonicException.MnemonicWordException, IOException {

        List<String> words = Arrays.asList(recoveryPhrase.trim().split("\\s+"));

        InputStream wis = HDWalletFactory.class.getClassLoader()
                .getResourceAsStream("wordlist/" + new Locale("en", "US").toString() + ".txt");

        if(wis == null){
            throw new MnemonicException.MnemonicWordException("cannot read BIP39 word list");
        }

        MnemonicCode mc = new MnemonicCode(wis, null);

        try {
            mc.check(words);
            return true;
        } catch (MnemonicException e) {
            return false;
        }
    }
}
