package piuk.blockchain.android.ui.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Arrays;

import io.reactivex.schedulers.Schedulers;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.answers.LauncherShortcutEvent;
import piuk.blockchain.android.data.answers.Logging;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.ui.receive.ReceiveQrActivity;

public class LauncherShortcutHelper {

    @SuppressWarnings("WeakerAccess")
    public static final String SHORTCUT_ID_COPY = "SHORTCUT_ID_COPY";
    public static final String SHORTCUT_ID_QR = "SHORTCUT_ID_QR";

    private Context context;
    private PayloadDataManager payloadDataManager;
    private ShortcutManager shortcutManager;

    public LauncherShortcutHelper(Context context, PayloadDataManager payloadDataManager, ShortcutManager shortcutManager) {
        this.context = context;
        this.payloadDataManager = payloadDataManager;
        this.shortcutManager = shortcutManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void generateReceiveShortcuts() {
        String receiveAccountName = payloadDataManager.getDefaultAccount().getLabel();
        payloadDataManager.getNextReceiveAddress(payloadDataManager.getDefaultAccountIndex())
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.newThread())
                .subscribe(receiveAddress -> {
                    shortcutManager.removeAllDynamicShortcuts();

                    Intent copyIntent = new Intent();
                    copyIntent.setAction(Intent.ACTION_SEND);
                    copyIntent.setType("text/plain");
                    copyIntent.putExtra(Intent.EXTRA_TEXT, receiveAddress);

                    ShortcutInfo copyShortcut = new ShortcutInfo.Builder(context, SHORTCUT_ID_COPY)
                            .setShortLabel(context.getString(R.string.shortcut_receive_copy_short))
                            .setLongLabel(context.getString(R.string.shortcut_receive_copy_long))
                            .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_copy))
                            .setIntent(copyIntent)
                            .build();

                    Intent qrIntent = new Intent(context, ReceiveQrActivity.class);
                    qrIntent.setAction(Intent.ACTION_VIEW);
                    qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, receiveAddress);
                    qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, receiveAccountName);

                    ShortcutInfo qrShortcut = new ShortcutInfo.Builder(context, SHORTCUT_ID_QR)
                            .setShortLabel(context.getString(R.string.shortcut_receive_qr_short))
                            .setLongLabel(context.getString(R.string.shortcut_receive_qr_long))
                            .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_scan))
                            .setIntent(qrIntent)
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(copyShortcut, qrShortcut));

                }, Throwable::printStackTrace);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void logShortcutUsed(String shortcutId) {
        shortcutManager.reportShortcutUsed(shortcutId);
        Logging.INSTANCE.logCustom(new LauncherShortcutEvent(shortcutId));
    }

}
