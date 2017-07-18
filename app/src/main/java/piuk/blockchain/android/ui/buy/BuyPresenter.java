package piuk.blockchain.android.ui.buy;

import android.support.annotation.Nullable;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.api.EnvironmentSettings;
import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.base.UiState;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by justin on 4/27/17.
 */

public class BuyPresenter extends BasePresenter<BuyView> {

    private AppUtil appUtil;
    private BuyDataManager buyDataManager;
    private PayloadDataManager payloadDataManager;
    private EnvironmentSettings environmentSettings;

    @Inject
    BuyPresenter(AppUtil appUtil,
                 BuyDataManager buyDataManager,
                 PayloadDataManager payloadDataManager,
                 EnvironmentSettings environmentSettings) {

        this.appUtil = appUtil;
        this.buyDataManager = buyDataManager;
        this.payloadDataManager = payloadDataManager;
        this.environmentSettings = environmentSettings;
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
        getView().setUiState(UiState.LOADING);

        getCompositeDisposable().add(payloadDataManager.loadNodes()
                .subscribe(loaded -> {
                    if (loaded) {
                        getCompositeDisposable().add(
                                buyDataManager
                                        .getWebViewLoginDetails()
                                        .subscribe(
                                                webViewLoginDetails -> getView().setWebViewLoginDetails(webViewLoginDetails),
                                                Throwable::printStackTrace));
                    } else {
                        // Not set up, most likely has a second password enabled
                        if (payloadDataManager.isDoubleEncrypted()) {
                            getView().showSecondPasswordDialog();
                            getView().setUiState(UiState.EMPTY);
                        } else {
                            generateMetadataNodes(null);
                        }
                    }
                }));
    }

    void generateMetadataNodes(@Nullable String secondPassword) {
        if (!payloadDataManager.validateSecondPassword(secondPassword)) {
            getView().showToast(R.string.invalid_password, ToastCustom.TYPE_ERROR);
            getView().showSecondPasswordDialog();
            getView().setUiState(UiState.EMPTY);
        } else {
            getCompositeDisposable().add(
                    payloadDataManager.generateNodes(secondPassword)
                            .subscribe(
                                    this::attemptPageSetup,
                                    throwable -> getView().setUiState(UiState.FAILURE)));
        }
    }

    String getCurrentServerUrl() {
        return environmentSettings.getExplorerUrl();
    }
}
