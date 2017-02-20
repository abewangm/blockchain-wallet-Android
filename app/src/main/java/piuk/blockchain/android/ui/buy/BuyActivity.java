package piuk.blockchain.android.ui.buy;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import info.blockchain.wallet.exceptions.MetadataException;
import info.blockchain.wallet.metadata.Metadata;
import info.blockchain.wallet.payload.PayloadManager;
import java.io.IOException;
import piuk.blockchain.android.R;
import piuk.blockchain.android.databinding.ActivityBuyBinding;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.util.annotations.Thunk;

public class BuyActivity extends BaseAuthActivity {

    private final int METADATA_TYPE_EXTERNAL = 3;

    @Thunk
    ActivityBuyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_buy);

        //Webview
//        binding.webview.

        //Wallet Credentials
//        PayloadManager.getInstance().getPayload().getGuid()
//        PayloadManager.getInstance().getPayload().getSharedKey()
//        PayloadManager.getInstance().getTempPassword().toString()

    }

    //metadata string from getMetadata, and set with setMetadata
    private Metadata getBuyMetadata() throws IOException, MetadataException {
        return new Metadata.Builder(PayloadManager.getInstance().getMasterKey(),
            METADATA_TYPE_EXTERNAL).build();
    }
}
