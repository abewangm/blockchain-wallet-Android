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
import piuk.blockchain.android.ui.receive.ReceiveQrActivity;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;

public class LauncherShortcutHelper {

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

                    ShortcutInfo copyShortcut = new ShortcutInfo.Builder(context, "receive")
                            .setShortLabel(context.getString(R.string.shortcut_receive_copy_short))
                            .setLongLabel(context.getString(R.string.shortcut_receive_copy_long))
                            .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_copy))
                            .setIntent(copyIntent)
                            .build();

                    Intent qrIntent = new Intent(context, ReceiveQrActivity.class);
                    qrIntent.setAction(Intent.ACTION_VIEW);
                    qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, receiveAddress);
                    qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, receiveAccountName);

                    ShortcutInfo qrShortcut = new ShortcutInfo.Builder(context, "qr")
                            .setShortLabel(context.getString(R.string.shortcut_receive_qr_short))
                            .setLongLabel(context.getString(R.string.shortcut_receive_qr_long))
                            .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_scan))
                            .setIntent(qrIntent)
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(copyShortcut, qrShortcut));

                }, Throwable::printStackTrace);
    }

}
