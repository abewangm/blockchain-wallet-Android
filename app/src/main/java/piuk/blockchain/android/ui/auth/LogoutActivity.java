package piuk.blockchain.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.util.OSUtil;
import piuk.blockchain.android.util.PrefsUtil;

/**
 * Created by adambennett on 04/08/2016.
 */

public class LogoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (getIntent().getAction().equals(AccessState.LOGOUT_ACTION)) {
                Intent intent = new Intent(this, WebSocketService.class);

                PrefsUtil prefsUtil = new PrefsUtil(this);

                // TODO: 04/01/2017 This is only supposed to be here until before Jun 30th
                prefsUtil.setValue(PrefsUtil.KEY_SURVEY_VISITS, 0);

                //When user logs out, assume onboarding has been completed
                prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true);

                if (new OSUtil(this).isServiceRunning(WebSocketService.class)) {
                    stopService(intent);
                }

                AccessState.getInstance().setIsLoggedIn(false);
                finishAffinity();
            }
        }
    }
}
