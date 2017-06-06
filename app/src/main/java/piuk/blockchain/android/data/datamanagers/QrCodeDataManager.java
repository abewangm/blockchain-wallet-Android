package piuk.blockchain.android.data.datamanagers;

import com.google.zxing.BarcodeFormat;

import android.graphics.Bitmap;
import android.util.Log;

import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.crypto.AESUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.ui.zxing.Contents;
import piuk.blockchain.android.ui.zxing.encode.QRCodeEncoder;

public class QrCodeDataManager {

    private static final int PAIRING_CODE_PBKDF2_ITERATIONS = 10;

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

    /**
     * Generates a pairing QR code in Bitmap format from a given password, sharedkey and encryption phrase to specified dimensions, wrapped in an
     * Observable. Will throw an error if the Bitmap is null.
     *
     * @param password          Wallet's plain text password
     * @param sharedKey         Wallet's plain text sharedkey
     * @param encryptionPhrase  The pairing encryption password
     * @param dimensions        The dimensions of the QR code to be returned
     * @return
     */
    public Observable<Bitmap> generatePairingCode(String guid, String password, String sharedKey, String encryptionPhrase, int dimensions) {
        return generatePairingCodeObservable(guid, password, sharedKey, encryptionPhrase, dimensions)
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

    private Observable<Bitmap> generatePairingCodeObservable(String guid, String password, String sharedKey, String encryptionPhrase, int dimensions) {
        return Observable.fromCallable(() -> {

            String pwHex = Hex.toHexString(password.getBytes("utf-8"));
            String encrypted = AESUtil.encrypt(sharedKey + "|" + pwHex, encryptionPhrase, PAIRING_CODE_PBKDF2_ITERATIONS);
            String qrData = "1|" + guid + "|" + encrypted;

            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(
                    qrData,
                    null,
                    Contents.Type.TEXT,
                    BarcodeFormat.QR_CODE.toString(),
                    dimensions);
            return qrCodeEncoder.encodeAsBitmap();
        });
    }
}
