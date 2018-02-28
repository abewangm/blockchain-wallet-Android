package piuk.blockchain.android.data.notifications;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import timber.log.Timber;

public class InstanceIdService extends FirebaseInstanceIdService {

    @Inject protected NotificationTokenManager notificationTokenManager;

    {
        Injector.getInstance().getAppComponent().inject(this);
    }

    public InstanceIdService() {
        Timber.d("InstanceIdService: constructor instantiated");
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Timber.d("Refreshed token: %s", refreshedToken);

        if (refreshedToken != null) {
            notificationTokenManager.storeAndUpdateToken(refreshedToken);
        }
    }
}
