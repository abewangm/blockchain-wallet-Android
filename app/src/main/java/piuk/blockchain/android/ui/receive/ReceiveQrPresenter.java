package piuk.blockchain.android.ui.receive;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.base.BasePresenter;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ReceiveQrPresenter extends BasePresenter<ReceiveQrView> {

    private static final int DIMENSION_QR_CODE = 600;

    private PayloadDataManager payloadDataManager;
    private QrCodeDataManager qrCodeDataManager;
    @VisibleForTesting String receiveAddressString;

    @Inject
    ReceiveQrPresenter(PayloadDataManager payloadDataManager, QrCodeDataManager qrCodeDataManager) {
        this.payloadDataManager = payloadDataManager;
        this.qrCodeDataManager = qrCodeDataManager;
    }

    @Override
    public void onViewReady() {
        Intent intent = getView().getPageIntent();

        if (intent != null
                && intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS) != null
                && intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL) != null) {

            // Show QR Code
            receiveAddressString = intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS);
            String labelString = intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL);

            getView().setAddressInfo(receiveAddressString);
            getView().setAddressLabel(labelString);

            qrCodeDataManager.generateQrCode("bitcoin:" + receiveAddressString, DIMENSION_QR_CODE)
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .subscribe(bitmap -> getView().setImageBitmap(bitmap),
                            throwable -> {
                                getView().showToast(R.string.shortcut_receive_qr_error, ToastCustom.TYPE_ERROR);
                                getView().finishActivity();
                            });

        } else {
            getView().finishActivity();
        }
    }

    void onCopyClicked() {
        getView().showClipboardWarning(receiveAddressString);
    }

    PayloadDataManager getPayloadDataManager() {
        return payloadDataManager;
    }

}
