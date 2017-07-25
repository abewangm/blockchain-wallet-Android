package piuk.blockchain.android.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;

public class SettingsActivity extends BaseAuthActivity {

    public static void start(Context context, @Nullable Bundle extras) {
        Intent starter = new Intent(context, SettingsActivity.class);
        if (extras != null) starter.putExtras(extras);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_general);
        setupToolbar(toolbar, R.string.action_settings);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

}
