package piuk.blockchain.android.data.datamanagers;

import com.google.zxing.BarcodeFormat;

import android.graphics.Bitmap;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.ui.zxing.Contents;
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder;

public class QrCodeDataManager {

    /**
     * Generates a QR code in Bitmap format from a given URI to specified dimensions, wrapped in an
     * Observable. Will throw an error if the Bitmap is null.
     *
     * @param uri        A string to be encoded
     * @param dimensions The dimensions of the QR code to be returned
     * @return An Observable wrapping the generate Bitmap operation
     */
    public Observable<Bitmap> generateQrCode(String uri, int dimensions) {
        return generateQrCodeObservable(uri, dimensions)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Bitmap> generateQrCodeObservable(String uri, int dimensions) {
        return Observable.fromCallable(() -> {
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(
                    uri,
                    null,
                    Contents.Type.TEXT,
                    BarcodeFormat.QR_CODE.toString(),
                    dimensions);
            return qrCodeEncoder.encodeAsBitmap();
        });
    }

}
