package piuk.blockchain.android.ui.buy;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.exchange.WebViewLoginDetails;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by justin on 4/27/17.
 */

@SuppressWarnings("WeakerAccess")
public class BuyViewModel extends BaseViewModel {

    private DataListener dataListener;

    @Inject protected AppUtil appUtil;
    @Inject protected BuyDataManager buyDataManager;
    @Inject protected PayloadDataManager payloadDataManager;
    @Inject protected EnvironmentSettings environmentSettings;

    public interface DataListener {

        void setUiState(@UiState.UiStateDef int uiState);

        void setWebViewLoginDetails(WebViewLoginDetails webViewLoginDetails);

        void showSecondPasswordDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

    }

    BuyViewModel(DataListener dataListener) {
        Injector.getInstance().getPresenterComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        attemptPageSetup();
    }

    Boolean isNewlyCreated() {
        return appUtil.isNewlyCreated();
    }

    void reloadExchangeDate() {
        buyDataManager.reloadExchangeData();
    }

    private void attemptPageSetup() {

        dataListener.setUiState(UiState.LOADING);

        compositeDisposable.add(payloadDataManager.loadNodes()
                .subscribe(loaded -> {
                    if (loaded) {
                        compositeDisposable.add(
                                buyDataManager
                                        .getWebViewLoginDetails()
                                        .subscribe(webViewLoginDetails -> {
                                                    dataListener.setWebViewLoginDetails(webViewLoginDetails);
                                                },
                                                Throwable::printStackTrace));
                    } else {
                        // Not set up, most likely has a second password enabled
                        if (payloadDataManager.isDoubleEncrypted()) {
                            dataListener.showSecondPasswordDialog();
                            dataListener.setUiState(UiState.EMPTY);
                        } else {
                            generateMetadataNodes(null);
                        }
                    }
                }));
    }

    void generateMetadataNodes(@Nullable String secondPassword) {

        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            dataListener.showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            dataListener.showSecondPasswordDialog();
            dataListener.setUiState(UiState.EMPTY);
        } else {
            compositeDisposable.add(
                    payloadDataManager.generateNodes(secondPassword)
                            .subscribe(() -> {
                                attemptPageSetup();
                            }, throwable -> {
                                dataListener.setUiState(UiState.FAILURE);
                            }));
        }
    }

    String getCurrentServerUrl() {
        return environmentSettings.getExplorerUrl();
    }
}
